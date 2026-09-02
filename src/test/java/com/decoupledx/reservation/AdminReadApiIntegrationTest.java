package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class AdminReadApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.update("TRUNCATE resource_blocks, reservations");
    }

    @Test
    void adminCanListAllReservationsWithStatusFilterAndPagination() throws Exception {
        createReservation("adminlist-user-1", FIELD_1, "2026-09-20T18:00:00", 90);
        createReservation("adminlist-user-2", FIELD_2, "2026-09-20T18:00:00", 90);

        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].customerId").isString());
    }

    @Test
    void customerReservationsSupportPaginationAndStatusFilter() throws Exception {
        createReservation("paged-user", FIELD_1, "2026-09-20T18:00:00", 90);

        mockMvc.perform(get("/api/reservations")
                        .with(customer("paged-user"))
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
    }

    @Test
    void adminCanReadPoliciesAndPricing() throws Exception {
        mockMvc.perform(get("/api/admin/booking-policy").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minDurationMinutes").isNumber())
                .andExpect(jsonPath("$.maxDurationMinutes").isNumber());

        mockMvc.perform(get("/api/admin/cancellation-policy").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineBeforeStartMinutes").isNumber());

        mockMvc.perform(get("/api/admin/pricing").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyPrice").isNumber())
                .andExpect(jsonPath("$.currency").value("PLN"));
    }

    @Test
    void adminCanListAllResourceBlocks() throws Exception {
        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_1, "2026-09-21T19:00:00", "2026-09-21T20:00:00", "report")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/resource-blocks").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/reservations").with(customer("regular-user")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/resource-blocks").with(customer("regular-user")))
                .andExpect(status().isForbidden());
    }

    private MvcResult createReservation(String subject, String resourceId, String startTime, int minutes)
            throws Exception {
        return mockMvc.perform(post("/api/reservations")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s", "startTime": "%s", "durationMinutes": %d}
                                """.formatted(resourceId, startTime, minutes)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String blockRequest(String resourceId, String startTime, String endTime, String reason) {
        return """
                {"resourceId": "%s", "startTime": "%s", "endTime": "%s", "reason": "%s"}
                """.formatted(resourceId, startTime, endTime, reason);
    }
}
