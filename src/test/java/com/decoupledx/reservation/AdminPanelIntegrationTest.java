package com.decoupledx.reservation;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.ZoneId;
import java.util.UUID;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Core-flow coverage only (testing convention): admin pages are gated to
 * ROLE_ADMIN sessions, load for admins, and the bypass-deadline cancel
 * redirects cleanly for success and failure. Page internals are not asserted;
 * reservation and cancellation business rules are covered black-box via the
 * REST API.
 */
@AutoConfigureMockMvc
class AdminPanelIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String UNKNOWN_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void adminPagesRedirectAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/reservations")).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminPagesAreForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/admin").with(webUser("alice")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reservations").with(webUser("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPagesLoadForAdmins() throws Exception {
        mockMvc.perform(get("/admin").with(webAdmin("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/reservations").with(webAdmin("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCancelAnyReservationRegardlessOfDeadline() throws Exception {
        // Run directly into a slot within the customer cancellation deadline
        // (fixed clock 2026-09-01T10:00Z; deadline 120 min) — inserted via SQL
        // because no in-hours bookable slot is that close to "now".
        UUID reservationId = insertReservationAt(
                "deadline-user", 12, 13);  // 10:30Z — inside the deadline

        MvcResult cancel = mockMvc.perform(post("/admin/reservations/{id}/cancel", reservationId)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .isEqualTo("/admin/reservations"))
                .andReturn();

        mockMvc.perform(get("/admin/reservations")
                        .session((org.springframework.mock.web.MockHttpSession) cancel.getRequest().getSession())
                        .with(webAdmin("admin")))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                "SELECT status FROM reservations WHERE id = ?", String.class, reservationId))
                .isEqualTo("CANCELLED");
    }

    @Test
    void adminCannotCancelElapsedReservations() throws Exception {
        // End time has fully passed relative to the fixed clock (2026-09-01T10:00Z):
        // nothing left to cancel, even administratively.
        UUID elapsedId = insertReservationAt("elapsed-user", 8, 9);  // 06:30Z, elapsed

        mockMvc.perform(post("/admin/reservations/{id}/cancel", elapsedId)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(jdbc.queryForObject(
                "SELECT status FROM reservations WHERE id = ?", String.class, elapsedId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void cancellingUnknownReservationRedirectsWithoutBreaking() throws Exception {
        MvcResult cancel = mockMvc.perform(post("/admin/reservations/{id}/cancel", UNKNOWN_ID)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(cancel.getResponse().getRedirectedUrl()).isEqualTo("/admin/reservations");
    }

    private UUID insertReservationAt(String customerId, int startHour, int endHour) {
        UUID reservationId = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO reservations
                            (id, resource_id, customer_id, start_time, end_time, status,
                             price_amount, price_currency, created_at, version)
                        VALUES (?, ?, ?, ?, ?, 'ACTIVE', 80.00, 'PLN', now(), 0)
                        """, reservationId, UUID.fromString(FIELD_1), customerId,
                venueTime(startHour), venueTime(endHour));
        return reservationId;
    }

    private java.time.OffsetDateTime venueTime(int hour) {
        return java.time.LocalDate.of(2026, 9, 1).atTime(hour, 30)
                .atZone(ZoneId.of("Europe/Warsaw")).toOffsetDateTime();
    }
}
