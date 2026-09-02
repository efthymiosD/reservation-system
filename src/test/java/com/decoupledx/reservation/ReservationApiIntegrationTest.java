package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    void cannotCancelSomeoneElsesReservation() throws Exception {
        String reservationId = createReservation("user-owner", FIELD_3, "2026-09-03T20:00:00", 60);

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .with(customer("user-intruder")))
                .andExpect(status().isNotFound());
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
