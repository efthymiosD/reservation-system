package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

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
class ReservationApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final String FIELD_3 = "a0000000-0000-0000-0000-000000000103";
    private static final String FIELD_4 = "a0000000-0000-0000-0000-000000000104";
    private static final String FIELD_5 = "a0000000-0000-0000-0000-000000000105";
    private static final String FIELD_6 = "a0000000-0000-0000-0000-000000000106";
    private static final ZoneId VENUE_ZONE = ZoneId.of("Europe/Warsaw");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void publicVenueInfoIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/venue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Five-a-Side Football Centre"))
                .andExpect(jsonPath("$.openingHours.MONDAY.opensAt").value("14:00:00"));
    }

    @Test
    void availabilityRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/availability")
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsReservationWithSnapshottedPrice() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_1, "2026-09-03T18:00:00", 90)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId").value(FIELD_1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.priceAmount").value(120.00))
                .andExpect(jsonPath("$.priceCurrency").value("PLN"));
    }

    @Test
    void rejectsSecondOverlappingReservationForSameCustomer() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-overlap"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_1, "2026-09-03T19:30:00", 60)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-overlap"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_6, "2026-09-03T20:00:00", 60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("CUSTOMER_HAS_OVERLAPPING_RESERVATION"));
    }

    @Test
    void rejectsOverlappingReservationFromDifferentCustomer() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_2, "2026-09-03T18:00:00", 90)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-b"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_2, "2026-09-03T18:30:00", 60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("RESOURCE_NO_LONGER_AVAILABLE"));
    }

    @Test
    void rejectsDurationOutsideConfiguredBounds() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_3, "2026-09-03T18:00:00", 45)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("INVALID_RESERVATION_DURATION"));
    }

    @Test
    void rejectsStartTimeOffIncrementGrid() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_3, "2026-09-03T18:15:00", 60)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("INVALID_START_TIME"));
    }

    @Test
    void rejectsPeriodOutsideOpeningHours() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("user-4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_3, "2026-09-03T22:30:00", 60)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("OUTSIDE_OPENING_HOURS"));
    }

    @Test
    void cancelledReservationFreesTheSlot() throws Exception {
        String reservationId = createReservation("user-cancel", FIELD_4, "2026-09-03T18:00:00", 90);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .with(customer("user-cancel")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/availability")
                        .with(customer("user-cancel"))
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resourceId == '" + FIELD_4 + "')]").exists());
    }

    @Test
    void cancellationIsEvaluatedAgainstCurrentlyConfiguredPolicy() throws Exception {
        String reservationId = createReservation("user-policy", FIELD_2, "2026-09-01T15:00:00", 60);

        mockMvc.perform(put("/api/admin/cancellation-policy")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadlineBeforeStartMinutes\": 240}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .with(customer("user-policy")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("CANCELLATION_DEADLINE_PASSED"));

        mockMvc.perform(put("/api/admin/cancellation-policy")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadlineBeforeStartMinutes\": 120}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .with(customer("user-policy")))
                .andExpect(status().isNoContent());
    }

    @Test
    void priceDoesNotChangeWhenPricingIsUpdated() throws Exception {
        String reservationId = createReservation("user-price", FIELD_5, "2026-09-03T18:00:00", 90);

        mockMvc.perform(put("/api/admin/pricing")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hourlyPrice\": 100.00, \"currency\": \"PLN\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .with(customer("user-price")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceAmount").value(120.00));

        mockMvc.perform(put("/api/admin/pricing")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hourlyPrice\": 80.00, \"currency\": \"PLN\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(put("/api/admin/cancellation-policy")
                        .with(customer("regular-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadlineBeforeStartMinutes\": 60}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pastStartTimeIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer("past-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(FIELD_1, "2026-08-30T18:00:00", 60)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("past")));
    }

    @Test
    void cannotCancelSomeoneElsesReservation() throws Exception {
        String reservationId = createReservation("user-owner", FIELD_3, "2026-09-03T20:00:00", 60);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .with(customer("user-intruder")))
                .andExpect(status().isNotFound());
    }

    @Test
    void canCancelIsReportedTrueWhileBeforeDeadline() throws Exception {
        String reservationId = createReservation("cc-user", FIELD_1, "2026-09-03T18:00:00", 60);

        mockMvc.perform(get("/api/reservations/{id}", reservationId).with(customer("cc-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.canCancel").value(true));
    }

    @Test
    void canCancelIsReportedFalseWithinDeadline() throws Exception {
        // Inserted directly because no valid bookable slot lies within the 120-minute
        // cancellation deadline of the fixed clock (2026-09-01T10:00Z; opening 14:00 local).
        UUID customerId = provisionCustomer("cc-deadline-user");
        insertReservation(UUID.fromString(FIELD_1), customerId,
                venueTime(LocalDate.of(2026, 9, 1), 12, 30),   // 10:30Z, inside the deadline
                venueTime(LocalDate.of(2026, 9, 1), 13, 30));

        MvcResult list = mockMvc.perform(get("/api/reservations").with(customer("cc-deadline-user")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(list.getResponse().getContentAsString()).get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("canCancel").asBoolean()).isFalse();
    }

    private UUID provisionCustomer(String subject) {
        jdbc.update("""
                INSERT INTO customers (customer_id, idp_subject)
                VALUES (?, ?)
                ON CONFLICT (idp_subject) DO NOTHING
                """, UUID.randomUUID(), subject);
        return jdbc.queryForObject(
                "SELECT customer_id FROM customers WHERE idp_subject = ?", UUID.class, subject);
    }

    private void insertReservation(UUID resourceId, UUID customerId, OffsetDateTime start, OffsetDateTime end) {
        jdbc.update("""
                INSERT INTO reservations
                    (id, resource_id, customer_id, start_time, end_time, status,
                     price_amount, price_currency, created_at, version)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', 80.00, 'PLN', now(), 0)
                """, UUID.randomUUID(), resourceId, customerId.toString(), start, end);
    }

    private OffsetDateTime venueTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(VENUE_ZONE).toOffsetDateTime();
    }

    private String createReservation(String subject, String resourceId, String startTime, int minutes)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(resourceId, startTime, minutes)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String createRequest(String resourceId, String startTime, int durationMinutes) {
        return """
                {"resourceId": "%s", "startTime": "%s", "durationMinutes": %d}
                """.formatted(resourceId, startTime, durationMinutes);
    }
}
