package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
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
 * Core-flow coverage only (testing convention): the advanced admin recurring-reservations
 * page is gated to ROLE_ADMIN, loads for admins, and the create/cancel flows
 * redirect cleanly and persist. Page internals are not asserted.
 */
@AutoConfigureMockMvc
class AdminRecurringReservationsPageIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String CUSTOMER = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recurringReservationsPageRedirectsAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin/recurring-reservations")).andExpect(status().is3xxRedirection());
    }

    @Test
    void recurringReservationsPageIsForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/admin/recurring-reservations").with(webUser("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void recurringReservationsPageLoadsForAdmins() throws Exception {
        mockMvc.perform(get("/admin/recurring-reservations").with(webAdmin("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateAnRecurringReservationFromThePage() throws Exception {
        MvcResult create = mockMvc.perform(post("/admin/recurring-reservations")
                        .with(webAdmin("admin"))
                        .with(csrf())
                        .param("resourceId", FIELD_1)
                        .param("customerId", CUSTOMER)
                        .param("weekday", "WEDNESDAY")
                        .param("startTime", "18:00")
                        .param("durationMinutes", "60")
                        .param("windowMonths", "3"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(create.getResponse().getRedirectedUrl()).isEqualTo("/admin/recurring-reservations");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM recurring_reservations", Integer.class)).isEqualTo(1);
    }

    @Test
    void adminCanCancelAnRecurringReservationFromThePage() throws Exception {
        UUID recurringReservationId = createRecurringReservationViaApi();

        MvcResult cancel = mockMvc.perform(post("/admin/recurring-reservations/{id}/cancel", recurringReservationId)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(cancel.getResponse().getRedirectedUrl()).isEqualTo("/admin/recurring-reservations");
        assertThat(jdbc.queryForObject(
                "SELECT status FROM recurring_reservations WHERE id = ?", String.class, recurringReservationId))
                .isEqualTo("CANCELLED");
    }

    @Test
    void materializeFromThePageRedirectsWithoutBreaking() throws Exception {
        createRecurringReservationViaApi();

        mockMvc.perform(post("/admin/recurring-reservations/materialize")
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .isEqualTo("/admin/recurring-reservations"));
    }

    private UUID createRecurringReservationViaApi() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s", "customerId": "%s", "weekday": "WEDNESDAY",
                                 "startTime": "18:00", "endTime": "19:00", "windowMonths": 3}
                                """.formatted(FIELD_1, CUSTOMER)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText());
    }
}