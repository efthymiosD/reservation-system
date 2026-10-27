package com.decoupledx.reservation;

import com.decoupledx.reservation.testinfra.JwtSupport;
import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Black-box coverage of the booking-request link workflow: public submit →
 * PENDING (without reservations/availability changes), the coach queue's
 * authorization, authoritative approval (exactly one reservation, safe on
 * conflicts) and rejection, link validation/revocation and dedupe.
 */
@AutoConfigureMockMvc
class BookingRequestIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final Pattern TOKEN_IN_SHARE_LINK = Pattern.compile("/booking-requests/new/([A-Za-z0-9_-]{22})$");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    com.decoupledx.reservation.webui.bookingrequest.BookingRequestRateLimitFilter rateLimitFilter;

    @BeforeEach
    void cleanTables() {
        jdbc.update("TRUNCATE booking_requests, booking_request_links");
        rateLimitFilter.reset();
    }

    @Test
    void publicFormCreatesPendingRequestWithoutReserving() throws Exception {
        String token = freshCoachLinkToken();

        MvcResult submit = mockMvc.perform(submissionBuilder(token)
                        .param("resourceId", FIELD_1)
                        .param("date", "2026-09-27")
                        .param("start", "18:00")
                        .param("durationMinutes", "90")
                        .param("customerName", "Ann Smith")
                        .param("contact", "+48 600 100 200")
                        .param("note", "mom of 2 kids")
                        .param("submissionKey", "bkrkey-test-0001-abcdef0123456789"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String ackLocation = submit.getResponse().getRedirectedUrl();
        assertThat(ackLocation)
                .as("flash error present: " + submit.getFlashMap().get("error"))
                .contains("/ack/bkrkey-test-0001-abcdef0123456789");

        // Follow it to see the unambiguous pending acknowledgement.
        mockMvc.perform(get(ackLocation.replace("http://localhost", "")))
                .andExpect(status().isOk());

        UUID requestId = jdbc.queryForObject("SELECT id FROM booking_requests", UUID.class);
        assertThat(jdbc.queryForMap(
                "SELECT status, reservation_id FROM booking_requests WHERE id = ?", requestId))
                .containsEntry("status", "PENDING")
                .containsEntry("reservation_id", null);
        // A request is NOT a reservation: the authoritative table stayed empty.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class))
                .isZero();
    }

    @Test
    void repeatSubmissionWithSameKeyIsIdempotent() throws Exception {
        String token = freshCoachLinkToken();

        for (int i = 0; i < 2; i++) {
            MvcResult result = mockMvc.perform(submissionBuilder(token)
                            .param("resourceId", FIELD_1)
                            .param("date", "2026-09-27")
                            .param("start", "18:00")
                            .param("durationMinutes", "90")
                            .param("customerName", "Ann Smith")
                            .param("contact", "+48 600 100 200")
                            .param("submissionKey", "bkrkey-test-0002-abcdef0123456789"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();
            assertThat(result.getResponse().getRedirectedUrl()).contains("/ack/bkrkey-test-0002-abcdef0123456789");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking_requests", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void coachApproveCreatesExactlyOneReservation() throws Exception {
        String token = freshCoachLinkToken();
        submitRequest(token, "bkrkey-approve-0003-abcdef0123");

        mockMvc.perform(post("/admin/booking-requests/{id}/approve", anyRequestId())
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM booking_requests", String.class))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject(
                "SELECT reservation_id IS NOT NULL FROM booking_requests", Boolean.class))
                .isTrue();

        // Repeated approve/reject submissions are safe: the outcome is clear.
        MvcResult retry = mockMvc.perform(post("/admin/booking-requests/{id}/approve", anyRequestId())
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String redirect = String.valueOf(retry.getResponse().getRedirectedUrl());
        assertThat(redirect).isEqualTo("/admin/booking-requests");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void coachRejectLeavesNoReservation() throws Exception {
        String token = freshCoachLinkToken();
        submitRequest(token, "bkrkey-reject-0004-abcdef0123");

        mockMvc.perform(post("/admin/booking-requests/{id}/reject", anyRequestId())
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM booking_requests", String.class))
                .isEqualTo("REJECTED");
    }

    @Test
    void approvalFailsSafelyWhenSlotBecameUnavailable() throws Exception {
        String token = freshCoachLinkToken();
        submitRequest(token, "bkrkey-late-0005-abcdef0123");

        // Sometime between submit and approve, a regular customer books the slot.
        mockMvc.perform(post("/api/reservations")
                        .with(JwtSupport.customer("late-bird"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s", "startTime": "2026-09-27T18:00:00", "durationMinutes": 90}
                                """.formatted(FIELD_1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/booking-requests/{id}/approve", anyRequestId())
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Authoritative booking wins; the request stays unresolved (PENDING):
        // no duplicate reservation was created.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reservations", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM booking_requests", String.class))
                .isEqualTo("PENDING");
    }

    @Test
    void unknownAndRevokedLinksAreRejectedFriendly() throws Exception {
        mockMvc.perform(get("/booking-requests/new/definitely-not-a-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking-requests/invalid"));
        String token = freshCoachLinkToken();

        mockMvc.perform(post("/admin/booking-requests/link/revoke")
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/booking-requests/new/" + token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booking-requests/invalid"));
        mockMvc.perform(submissionBuilder(token).param("submissionKey", "bkrkey-anything-abcdef01234567"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void regeneratingTwiceKeepsExactlyOneActiveLink() throws Exception {
        freshCoachLinkToken();
        freshCoachLinkToken();

        Integer activeLinks = jdbc.queryForObject(
                "SELECT count(*) FROM booking_request_links WHERE revoked_at IS NULL", Integer.class);
        assertThat(activeLinks).isEqualTo(1);
        // Older tokens are dead: their hash rows exist but are revoked.
        Integer allLinks = jdbc.queryForObject(
                "SELECT count(*) FROM booking_request_links", Integer.class);
        assertThat(allLinks).isEqualTo(2);
    }

    @Test
    void publicUsersCannotManageOrInspectTheQueue() throws Exception {
        mockMvc.perform(get("/admin/booking-requests").with(webUser("alice")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/booking-requests"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/booking-requests/link")
                        .with(webUser("alice")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicSubmissionsAreThrottled() throws Exception {
        String token = freshCoachLinkToken();

        MvcResult last = null;
        for (int i = 1; i <= 7; i++) {
            last = mockMvc.perform(submissionBuilder(token)
                            .param("resourceId", FIELD_1)
                            .param("date", "2026-09-27")
                            .param("start", "18:00")
                            .param("durationMinutes", "90")
                            .param("customerName", "Bulk Ann " + i)
                            .param("contact", "+48 600 100 200")
                            .param("submissionKey", "bkrkey-bulk-00-abcdef0123-" + i))
                    .andReturn();
        }
        assertThat(last.getResponse().getStatus()).isEqualTo(429);
    }

    private String freshCoachLinkToken() throws Exception {
        MvcResult issue = mockMvc.perform(post("/admin/booking-requests/link")
                        .with(webAdmin("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String shareLink = String.valueOf(issue.getFlashMap().get("freshShareLink"));
        Matcher matcher = TOKEN_IN_SHARE_LINK.matcher(shareLink);
        assertThat(matcher.find()).as("fresh share link has a token: " + shareLink).isTrue();
        return matcher.group(1);
    }

    private void submitRequest(String token, String submissionKey) throws Exception {
        mockMvc.perform(submissionBuilder(token)
                        .param("resourceId", FIELD_1)
                        .param("date", "2026-09-27")
                        .param("start", "18:00")
                        .param("durationMinutes", "90")
                        .param("customerName", "Ann Smith")
                        .param("contact", "+48 600 100 200")
                        .param("submissionKey", submissionKey))
                .andExpect(status().is3xxRedirection());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submissionBuilder(String token) {
        return post("/booking-requests/new/" + token).with(webAdmin("admin")).with(csrf());
    }

    private UUID anyRequestId() {
        return jdbc.queryForObject("SELECT id FROM booking_requests", UUID.class);
    }
}
