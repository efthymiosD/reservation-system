package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Track B contract: an administrator may place several bookings for the same
 * hour on behalf of a customer (different pitches) — the per-customer overlap
 * exclusion constraint deliberately ignores admin-override rows. The pitch
 * invariant stays hard (one pitch = one booking per window). The customer
 * self-service path keeps the stricter rule.
 */
@AutoConfigureMockMvc
class AdminOnBehalfBookingIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.update("TRUNCATE recurring_reservations, reservations");
    }

    @Test
    void adminPlacesSeveralBookingsForTheSameHourOnDifferentPitches() throws Exception {
        bookOnBehalf(FIELD_1, "2026-09-20T18:00:00", 201);
        bookOnBehalf(FIELD_2, "2026-09-20T18:00:00", 201);

        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM reservations WHERE start_time = ? AND admin_override = TRUE AND customer_id = ?",
                Integer.class,
                java.sql.Timestamp.from(LocalDateTime.of(2026, 9, 20, 18, 0)
                        .atZone(java.time.ZoneId.of("Europe/Warsaw")).toInstant()),
                CUSTOMER_ID);
        assertThat(rows).isEqualTo(2);
    }

    @Test
    void adminConfiguredCustomerKeepsTheirOwnOverlapRuleWhenBookingThemselves() throws Exception {
        // A regular customer booking into the same hour as an admin-placed
        // booking on a free pitch is still possible (admin rows are exempt
        // from the constraint), but the customer's own second self-booking at
        // the same hour must be refused.
        bookOnBehalf(FIELD_1, "2026-09-20T18:00:00", 201);
        mockMvc.perform(post("/api/reservations")
                        .with(customer("overlap-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s",
                                 "startTime": "2026-09-20T18:30:00",
                                 "durationMinutes": 60}
                                """.formatted(FIELD_2)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reservations")
                        .with(customer("overlap-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s",
                                 "startTime": "2026-09-20T18:30:00",
                                 "durationMinutes": 60}
                                """.formatted(FIELD_2)))
                .andExpect(status().isConflict());
    }

    @Test
    void onePitchStillCannotHostTwoBookingsInTheSameWindow() throws Exception {
        bookOnBehalf(FIELD_1, "2026-09-20T18:00:00", 201);
        bookOnBehalf(FIELD_1, "2026-09-20T18:30:00", 409);
    }

    @Test
    void administratorBookingForThemselvesMayHoldTwoFieldsInTheSameHour() throws Exception {
        // The public reserve page books for the current player; an admin
        // acting as the current player gets the relaxed rule too.
        mockMvc.perform(post("/api/reservations")
                        .with(admin("admin-self"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfBookingRequest(FIELD_1, "2026-09-20T18:00:00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reservations")
                        .with(admin("admin-self"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfBookingRequest(FIELD_2, "2026-09-20T18:00:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void reservePageShowsNoHeldHintForAnAdministratorWithAnOverlappingBooking() throws Exception {
        bookOnBehalf(FIELD_1, "2026-09-20T18:00:00", 201);
        // The admin owns an overlapping booking, yet the page must not render
        // the "you already hold this slot" blocked variant of the Reserve
        // button (tooltip spans are the only data-bootstrap tooltips there).
        String adminPage = mockMvc.perform(get("/reserve")
                        .param("date", "2026-09-20")
                        .param("start", "18:00")
                        .param("durationMinutes", "60")
                        .with(admin("admin-self")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminPage).doesNotContain("data-bs-toggle=\"tooltip\"");

        // Same slot, a plain customer who holds a booking: the hint appears.
        mockMvc.perform(post("/api/reservations")
                        .with(customer("hint-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfBookingRequest(FIELD_2, "2026-09-20T18:00:00")))
                .andExpect(status().isCreated());
        String customerPage = mockMvc.perform(get("/reserve")
                        .param("date", "2026-09-20")
                        .param("start", "18:00")
                        .param("durationMinutes", "60")
                        .with(customer("hint-customer")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(customerPage).contains("data-bs-toggle=\"tooltip\"");
    }

    @Test
    void customerSelfBookingKeepsTheSameHourOverlapBlock() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("self-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfBookingRequest(FIELD_1, "2026-09-20T18:00:00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reservations")
                        .with(customer("self-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfBookingRequest(FIELD_2, "2026-09-20T18:00:00")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminEndpointRejectsCustomers() throws Exception {
        mockMvc.perform(post("/api/admin/reservations")
                        .with(customer("no-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(FIELD_1, "2026-09-20T18:00:00")))
                .andExpect(status().isForbidden());
    }

    private void bookOnBehalf(String resourceId, String startTime, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/admin/reservations")
                        .with(admin("acts-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(resourceId, startTime)))
                .andExpect(status().is(expectedStatus));
    }

    private static String selfBookingRequest(String resourceId, String startTime) {
        return """
                {"resourceId": "%s",
                 "startTime": "%s",
                 "durationMinutes": 60}
                """.formatted(resourceId, startTime);
    }

    private static String request(String resourceId, String startTime) {
        return """
                {"resourceId": "%s",
                 "customerId": "%s",
                 "startTime": "%s",
                 "durationMinutes": 60}
                """.formatted(resourceId, CUSTOMER_ID, startTime);
    }
}
