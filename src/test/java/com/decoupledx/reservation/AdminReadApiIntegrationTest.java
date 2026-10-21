package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        jdbc.update("TRUNCATE recurring_reservations, reservations");
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
    void adminCustomerAndDateWindowFiltersWorkInConjunction() throws Exception {
        customerWithName("filterset-anna", "Ann Smith", "2026-09-20T18:00:00", FIELD_1);
        customerWithName("filterset-bob", "Bob Jones", "2026-09-21T18:00:00", FIELD_1);

        // Ann books 2026-09-20 18:00 Warsaw == 16:00Z (CEST); the name plus
        // the window match exactly one row.
        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("customer", "Ann Smith")
                        .param("status", "ACTIVE")
                        .param("startTimeFrom", "2026-09-20T00:00:00Z")
                        .param("startTimeTo", "2026-09-21T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].customerId").value(
                        jdbc.queryForObject(
                                "SELECT customer_id FROM customers WHERE idp_subject = 'filterset-anna'",
                                String.class)));

        // The same window without the customer constraint: both days.
        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("status", "ACTIVE")
                        .param("startTimeFrom", "2026-09-20T00:00:00Z")
                        .param("startTimeTo", "2026-09-22T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void adminDateWindowIsStartInclusiveAndEndExclusive() throws Exception {
        // Carl books 2026-09-22 18:00 Warsaw == 16:00Z.
        customerWithName("filterset-carl", "Carl Bean", "2026-09-22T18:00:00", FIELD_1);

        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("startTimeFrom", "2026-09-22T16:00:00Z")
                        .param("startTimeTo", "2026-09-22T16:01:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("startTimeFrom", "2026-09-22T16:00:00.000001Z")
                        .param("startTimeTo", "2026-09-22T16:01:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // The exclusive upper bound at exactly the booking start: nothing.
        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("startTimeFrom", "2026-09-22T00:00:00Z")
                        .param("startTimeTo", "2026-09-22T16:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void adminFilterForUnknownCustomerYieldsExplicitEmptyPage() throws Exception {
        createReservation("filterset-dan", FIELD_1, "2026-09-23T18:00:00", 90);

        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("customer", "Nobody Known"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    /** Books via the API, then stores a directory display name for the subject. */
    private UUID customerWithName(String subject, String displayName, String startTime, String field)
            throws Exception {
        createReservation(subject, field, startTime, 90);
        UUID customerId = jdbc.queryForObject(
                "SELECT customer_id FROM customers WHERE idp_subject = ?", UUID.class, subject);
        jdbc.update("UPDATE customers SET display_name = ? WHERE customer_id = ?", displayName, customerId);
        return customerId;
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
    void adminCanListAllRecurringReservations() throws Exception {
        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, "WEDNESDAY", "19:00", "20:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/recurring-reservations").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].weekday").value("WEDNESDAY"));
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/reservations").with(customer("regular-user")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/recurring-reservations").with(customer("regular-user")))
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

    private String recurringReservationRequest(String resourceId, String weekday, String startTime, String endTime) {
        return """
                {"resourceId": "%s",
                 "customerId": "11111111-1111-1111-1111-111111111111",
                 "weekday": "%s",
                 "startTime": "%s",
                 "endTime": "%s",
                 "windowMonths": 1}
                """.formatted(resourceId, weekday, startTime, endTime);
    }
}
