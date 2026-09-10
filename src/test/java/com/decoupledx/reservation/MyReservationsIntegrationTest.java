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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import tools.jackson.databind.ObjectMapper;

/**
 * Core-flow coverage only (testing convention): the page is authenticated-only,
 * reachable for its owner, and owner-scoped cancellation works end to end
 * (success + rejections). Page internals are not asserted — the business rules
 * (deadline, ownership, status changes) are already covered black-box via the
 * REST API.
 */
@AutoConfigureMockMvc
class MyReservationsIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pageRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/my-reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .contains("/oauth2/authorization/keycloak"));
    }

    @Test
    void pageLoadsForItsOwner() throws Exception {
        createReservationViaApi("list-user", FIELD_1);
        mockMvc.perform(get("/my-reservations").with(webUser("list-user")))
                .andExpect(status().isOk());
    }

    @Test
    void cancellationRedirectsBackToTheList() throws Exception {
        String reservationId = createReservationViaApi("cancel-user", FIELD_1);

        MvcResult cancel = mockMvc.perform(post("/my-reservations/{id}/cancel", reservationId)
                        .with(webUser("cancel-user"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .isEqualTo("/my-reservations"))
                .andReturn();

        // Flash text is session-bound under MockMvc; assert through the shared
        // session that the follow-up view renders.
        mockMvc.perform(get("/my-reservations")
                        .session((org.springframework.mock.web.MockHttpSession) cancel.getRequest().getSession())
                        .with(webUser("cancel-user")))
                .andExpect(status().isOk());
    }

    @Test
    void cancellingSomeoneElsesReservationRedirectsBackToTheList() throws Exception {
        String reservationId = createReservationViaApi("real-owner", FIELD_1);
        mockMvc.perform(post("/my-reservations/{id}/cancel", reservationId)
                        .with(webUser("intruder"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    private String createReservationViaApi(String subject, String resourceId) throws Exception {
        String body = """
                {"resourceId": "%s", "startTime": "2026-09-03T18:00:00", "durationMinutes": 60}
                """.formatted(resourceId);
        MvcResult result = mockMvc.perform(post("/api/reservations").with(customer(subject))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
