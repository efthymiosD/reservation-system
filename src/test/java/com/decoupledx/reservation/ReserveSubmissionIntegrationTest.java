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
 * Core-flow coverage only (testing convention): the submission and confirmation
 * redirections, ownership enforcement, and clean rejection handling. Page
 * internals are not asserted — the business rules themselves are covered by the
 * REST API and domain-level tests.
 */
@AutoConfigureMockMvc
class ReserveSubmissionIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_3 = "a0000000-0000-0000-0000-000000000103";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void reservePageRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/reserve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/oauth2/authorization/keycloak"));
    }

    @Test
    void submittingRedirectsToAnAccessibleConfirmation() throws Exception {
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

        mockMvc.perform(get(location).with(webUser("booker")))
                .andExpect(status().isOk());
    }

    @Test
    void staleAvailabilityRedirectsBackToTheMap() throws Exception {
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
        mockMvc.perform(get("/reserve?date=2026-09-03&start=18:00&durationMinutes=90")
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("slow-finger")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidSlotRedirectsBackToTheMap() throws Exception {
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("booker"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "45"))  // not offered by the policy
                .andExpect(status().is3xxRedirection())
                .andReturn();
        mockMvc.perform(get(submit.getResponse().getRedirectedUrl())
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("booker")))
                .andExpect(status().isOk());
    }

    @Test
    void pastStartTimeRedirectsBackToTheMap() throws Exception {
        MvcResult submit = mockMvc.perform(post("/reserve")
                        .with(webUser("booker"))
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-08-30")
                        .param("start", "18:00")
                        .param("durationMinutes", "60"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        mockMvc.perform(get(submit.getResponse().getRedirectedUrl())
                        .session((org.springframework.mock.web.MockHttpSession) submit.getRequest().getSession())
                        .with(webUser("booker")))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousSubmissionRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/reserve")
                        .with(csrf())
                        .param("resourceId", FIELD_3)
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "60"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
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

    @Test
    void allFieldsUnavailableStillLoadsThePage() throws Exception {
        for (int i = 1; i <= 6; i++) {
            jdbc.update("""
                    INSERT INTO resource_blocks
                        (id, resource_id, start_time, end_time, reason, status, created_at, version)
                    VALUES (?, ?, ?, ?, 'maintenance', 'ACTIVE', now(), 0)
                    """, UUID.randomUUID(),
                    UUID.fromString("a0000000-0000-0000-0000-00000000010" + i),
                    venueTime(18, 0), venueTime(19, 0));
        }
        mockMvc.perform(get("/reserve?date=2026-09-01&start=18:00&durationMinutes=60")
                        .with(webUser("alice")))
                .andExpect(status().isOk());
    }

    private String createReservationViaApi(String subject) throws Exception {
        String body = """
                {"resourceId": "%s", "startTime": "2026-09-03T20:00:00", "durationMinutes": 60}
                """.formatted(FIELD_3);
        MvcResult result = mockMvc.perform(post("/api/reservations").with(customer(subject))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private java.time.OffsetDateTime venueTime(int hour, int minute) {
        return java.time.LocalDate.of(2026, 9, 1).atTime(hour, minute)
                .atZone(java.time.ZoneId.of("Europe/Warsaw")).toOffsetDateTime();
    }
}
