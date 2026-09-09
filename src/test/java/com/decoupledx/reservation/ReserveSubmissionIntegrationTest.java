package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import tools.jackson.databind.ObjectMapper;

/**
 * T6: submitting a reservation from the web page and the confirmation page.
 * Success redirects to a personal confirmation; business rejections (stale
 * availability, overlaps, invalid slots) redirect back to the map with a
 * friendly message and freshly rendered availability. Authentication and CSRF
 * are enforced server-side.
 */
@AutoConfigureMockMvc
class ReserveSubmissionIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_3 = "a0000000-0000-0000-0000-000000000103";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void pageRendersSelectionWiring() throws Exception {
        // The page is authenticated-only: anonymous visitors are redirected to the
        // Keycloak authorization endpoint.
        mockMvc.perform(get("/reserve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/oauth2/authorization/keycloak"));

        String authenticatedHtml = bodyOf(get("/reserve").with(webUser("alice")));
        assertThat(authenticatedHtml).contains("/js/reserve.js");
        assertThat(authenticatedHtml).contains("id=\"reserve-button\"");
        assertThat(authenticatedHtml).contains("disabled");
        assertThat(authenticatedHtml).contains("id=\"hidden-resource-id\"");
        // Regression guard: the page must render completely — the POST form's CSRF
        // field forces a session; rendering it anonymously used to truncate the page.
        assertThat(authenticatedHtml).contains("</html>");
    }

    @Test
    void allFieldsUnavailableDisablesAndGreysTheButton() throws Exception {
        // Another customer holds every field for the slot: reservation is not
        // possible — the button is disabled, grey, and carries a tooltip.
        for (int i = 1; i <= 6; i++) {
            jdbc.update("""
                    INSERT INTO resource_blocks
                        (id, resource_id, start_time, end_time, reason, status, created_at, version)
                    VALUES (?, ?, ?, ?, 'maintenance', 'ACTIVE', now(), 0)
                    """, UUID.randomUUID(),
                    UUID.fromString("a0000000-0000-0000-0000-00000000010" + i),
                    venueTime(18, 0), venueTime(19, 0));
        }

        String html = flat(bodyOf(get("/reserve?date=2026-09-01&start=18:00&durationMinutes=60")
                .with(webUser("alice"))));

        assertThat(html).contains("btn-secondary");
        assertThat(html).contains(
                "Reservation is not possible: all fields are reserved or blocked for this time window.");
        assertThat(html).contains("disabled");
        assertThat(html).doesNotContain("id=\"reserve-button\"");
        // The map still renders the states and the price card stays visible.
        assertThat(html).contains("resource--blocked");
        assertThat(html).contains("80.00");
    }

    @Test
    void submittingReservesTheSlotAndShowsConfirmation() throws Exception {
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("booker"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = submit.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/reservations/").endsWith("/confirmation");
        String reference = location.split("/reservations/")[1].split("/")[0];

        MvcResult confirmation = mockMvc.perform(get(location).with(webUser("booker")))
                .andExpect(status().isOk())
                .andReturn();
        String html = confirmation.getResponse().getContentAsString();
        assertThat(html).contains("Reservation confirmed");
        assertThat(html).contains("Field 3");
        assertThat(html).contains("3 September 2026");
        assertThat(html).contains("18:00");
        assertThat(html).contains("19:30");
        assertThat(html).contains("120.00");
        assertThat(html).contains("PLN");
        assertThat(html).contains("At the venue");
        assertThat(html).contains(reference);
    }

    @Test
    void staleAvailabilityRedirectsBackWithFreshMap() throws Exception {
        // Someone else books the slot first.
        String reservation = """
                {"resourceId": "%s", "startTime": "2026-09-03T18:00:00", "durationMinutes": 90}
                """.formatted(FIELD_3);
        mockMvc.perform(post("/api/reservations").with(customer("fastest-finger"))
                        .contentType(APPLICATION_JSON).content(reservation))
                .andExpect(status().isCreated());

        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("slow-finger"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(submit.getResponse().getRedirectedUrl())
                .isEqualTo("/reserve?date=2026-09-03&start=18:00&durationMinutes=90");

        // The redirect target renders the refreshed map: the field is now reserved.
        MvcResult refreshed = mockMvc.perform(get("/reserve?date=2026-09-03&start=18:00&durationMinutes=90")
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("slow-finger")))
                .andExpect(status().isOk())
                .andReturn();
        String html = flat(refreshed.getResponse().getContentAsString());
        assertThat(html).contains("resource--reserved");
        assertThat(html).contains("no longer available");
    }

    @Test
    void pageHintsAboutOwnOverlappingReservation() throws Exception {
        // The web session's sub equals the API subject, so this is the SAME customer
        // that will later view the page.
        String reservation = """
                {"resourceId": "%s", "startTime": "2026-09-03T18:00:00", "durationMinutes": 90}
                """.formatted(FIELD_3);
        mockMvc.perform(post("/api/reservations").with(customer("booker"))
                        .contentType(APPLICATION_JSON).content(reservation))
                .andExpect(status().isCreated());

        MvcResult mine = mockMvc.perform(get("/reserve?date=2026-09-03&start=18:00&durationMinutes=90")
                        .with(webUser("booker")))
                .andExpect(status().isOk())
                .andReturn();
        String html = flat(mine.getResponse().getContentAsString());
        // The hint is a tooltip on the disabled Reserve button — the button keeps
        // its original 'Reserve' label and there is no separate warning alert.
        assertThat(html).contains("data-bs-toggle=\"tooltip\"");
        assertThat(html).contains("You already have a reservation for this slot: Field 3 (18:00 – 19:30)");
        assertThat(html).contains(">Reserve</button>");
        assertThat(html).contains("disabled");
        assertThat(html).doesNotContain("Already reserved for this slot");

        // Other customers get normal Reserve-button wiring and no tooltip.
        MvcResult others = mockMvc.perform(get("/reserve?date=2026-09-03&start=18:00&durationMinutes=90")
                        .with(webUser("someone-else")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(flat(others.getResponse().getContentAsString()))
                .doesNotContain("data-bs-toggle=\"tooltip\"")
                .contains("id=\"reserve-button\"");
    }

    @Test
    void overlapWithOwnReservationIsRejectedCleanly() throws Exception {
        String reservation = """
                {"resourceId": "%s", "startTime": "2026-09-03T18:00:00", "durationMinutes": 60}
                """.formatted(FIELD_3);
        mockMvc.perform(post("/api/reservations").with(customer("dupe-user"))
                        .contentType(APPLICATION_JSON).content(reservation))
                .andExpect(status().isCreated());

        // Same customer (same sub via the web session) books the same slot again.
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("dupe-user"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "60"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MvcResult refreshed = mockMvc.perform(get(submit.getResponse().getRedirectedUrl())
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("dupe-user")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(flat(refreshed.getResponse().getContentAsString()))
                .contains("You already have a reservation overlapping this time");
    }

    @Test
    void invalidSlotIsRejectedWithGuidance() throws Exception {
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("booker"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "45"))  // not offered by the policy
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MvcResult refreshed = mockMvc.perform(get(submit.getResponse().getRedirectedUrl())
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("booker")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(flat(refreshed.getResponse().getContentAsString()))
                .contains("That duration is not offered");
    }

    @Test
    void pastStartTimeIsRejectedWithGuidance() throws Exception {
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("booker"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-08-30")
                        .param("start", "18:00")
                        .param("durationMinutes", "60"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MvcResult refreshed = mockMvc.perform(get(submit.getResponse().getRedirectedUrl())
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("booker")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(flat(refreshed.getResponse().getContentAsString()))
                .contains("That start time is in the past");
    }

    @Test
    void anonymousSubmissionRedirectsToLogin() throws Exception {
        // CSRF passes (MockMvc provides the token), then authentication redirects
        // to the Keycloak authorization endpoint — reserving needs a session.
        mockMvc.perform(post("/reserve")
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "60"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getRedirectedUrl())
                        .contains("/oauth2/authorization/keycloak"));
    }

    @Test
    void confirmationOfSomeoneElsesReservationRedirectsBack() throws Exception {
        String reservationId = createReservationViaApi("owner-x");
        MvcResult result = mockMvc.perform(get("/reservations/{id}/confirmation", reservationId)
                        .with(webUser("intruder")))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/reserve");
    }

    private java.time.OffsetDateTime venueTime(int hour, int minute) {
        return java.time.LocalDate.of(2026, 9, 1).atTime(hour, minute)
                .atZone(java.time.ZoneId.of("Europe/Warsaw")).toOffsetDateTime();
    }

    private String createReservationViaApi(String subject) throws Exception {        String body = """
                {"resourceId": "%s", "startTime": "2026-09-03T20:00:00", "durationMinutes": 60}
                """.formatted(FIELD_3);
        MvcResult result = mockMvc.perform(post("/api/reservations").with(customer(subject))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String bodyOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString();
    }

    private static String flat(String html) {
        return html.replaceAll("\\s+", " ");
    }
}
