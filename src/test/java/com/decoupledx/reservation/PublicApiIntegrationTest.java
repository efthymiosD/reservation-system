package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
class PublicApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final ZoneId VENUE_ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate SLOT_DATE = LocalDate.of(2026, 9, 3);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicBookingPolicyIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/booking-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minDurationMinutes").value(60))
                .andExpect(jsonPath("$.maxDurationMinutes").value(120))
                .andExpect(jsonPath("$.durationStepMinutes").value(30))
                .andExpect(jsonPath("$.startTimeStepMinutes").value(30))
                .andExpect(jsonPath("$.maxAdvanceBooking").value("P1M"));
    }

    @Test
    void availabilityMapIsAccessibleWithoutAuthenticationAndShowsAllResources() throws Exception {
        mockMvc.perform(get("/api/public/availability/map")
                        .param("date", "2026-09-03")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-09-03"))
                .andExpect(jsonPath("$.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.resources.length()").value(6))
                .andExpect(jsonPath("$.resources[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.resources[0].priceAmount").value(120.00))
                .andExpect(jsonPath("$.resources[0].priceCurrency").value("PLN"));
    }

    @Test
    void availabilityMapMarksReservedAndBlockedResources() throws Exception {
        createReservation(FIELD_1, "map-user", 90);
        insertBlock(UUID.fromString(FIELD_2), venueTime(SLOT_DATE, 18, 0), venueTime(SLOT_DATE, 19, 30));

        JsonNode resources = fetchAvailabilityMap(SLOT_DATE, "18:00", 90);

        assertThat(statusOf(resources, FIELD_1)).isEqualTo("RESERVED");
        assertThat(statusOf(resources, FIELD_2)).isEqualTo("BLOCKED");
        assertThat(countWithStatus(resources, "AVAILABLE")).isEqualTo(4);
    }

    private MvcResult createReservation(String resourceId, String subject, int durationMinutes) throws Exception {
        String body = """
                {"resourceId": "%s", "startTime": "2026-09-03T18:00:00", "durationMinutes": %d}
                """.formatted(resourceId, durationMinutes);
        return mockMvc.perform(post("/api/reservations")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
    }

    private JsonNode fetchAvailabilityMap(LocalDate date, String start, int durationMinutes) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/availability/map")
                        .param("date", date.toString())
                        .param("start", start)
                        .param("durationMinutes", String.valueOf(durationMinutes)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("resources");
    }

    private String statusOf(JsonNode resources, String resourceId) {
        return streamOf(resources)
                .filter(resource -> resource.get("resourceId").asText().equals(resourceId))
                .findFirst()
                .map(resource -> resource.get("status").asText())
                .orElseThrow(() -> new AssertionError("resource not in map: " + resourceId));
    }

    private long countWithStatus(JsonNode resources, String status) {
        return streamOf(resources).filter(resource -> resource.get("status").asText().equals(status)).count();
    }

    private Stream<JsonNode> streamOf(JsonNode array) {
        List<JsonNode> nodes = new ArrayList<>();
        array.forEach(nodes::add);
        return nodes.stream();
    }

    private void insertBlock(UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
        jdbc.update("""
                INSERT INTO resource_blocks
                    (id, resource_id, start_time, end_time, reason, status, created_at, version)
                VALUES (?, ?, ?, ?, 'maintenance', 'ACTIVE', now(), 0)
                """, UUID.randomUUID(), resourceId, start, end);
    }

    private OffsetDateTime venueTime(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(VENUE_ZONE).toOffsetDateTime();
    }
}
