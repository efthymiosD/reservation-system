package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

/**
 * Black-box coverage of the recurring reservations admin API: creation
 * materializes the upcoming occurrences immediately, cancellation releases
 * every future reservation the recurringReservation had produced, conflicting slots are
 * skipped instead of failing, and the materializer endpoint is idempotent.
 */
@AutoConfigureMockMvc
class RecurringReservationApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final String CUSTOMER_A = "11111111-1111-1111-1111-111111111111";
    private static final String CUSTOMER_B = "22222222-2222-2222-2222-222222222222";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanTables() {
        jdbc.update("TRUNCATE recurring_reservations, reservations");
    }

    @Test
    void createRecurringReservationMaterializesUpcomingOccurrencesImmediately() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, CUSTOMER_A, "WEDNESDAY", "18:00", "19:30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.weekday").value("WEDNESDAY"))
                .andExpect(jsonPath("$.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.endTime").value("19:30:00"))
                .andExpect(jsonPath("$.windowMonths").value(1))
                .andExpect(jsonPath("$.nextOccurrence").value("2026-09-01"))
                .andReturn();

        UUID recurringReservationId = recurringReservationIdOf(created);

        // From 2026-09-02 through the 1-month advance window: 5 Wednesdays.
        assertThat(activeReservationsFor(recurringReservationId)).isEqualTo(5);

        mockMvc.perform(get("/api/admin/reservations")
                        .with(admin("admin"))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].recurringReservationId").value(recurringReservationId.toString()));
    }

    @Test
    void cancellingRecurringReservationReleasesEveryFutureReservation() throws Exception {
        UUID recurringReservationId = createRecurringReservation(FIELD_1, CUSTOMER_A, "WEDNESDAY", "18:00", "19:00");
        assertThat(activeReservationsFor(recurringReservationId)).isEqualTo(5);

        mockMvc.perform(post("/api/admin/recurring-reservations/{id}/cancel", recurringReservationId)
                        .with(admin("admin")))
                .andExpect(status().isNoContent());

        assertThat(activeReservationsFor(recurringReservationId)).isEqualTo(0);
        assertThat(cancelledReservationsFor(recurringReservationId)).isEqualTo(5);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM recurring_reservations WHERE id = ?", String.class, recurringReservationId))
                .isEqualTo("CANCELLED");

        mockMvc.perform(get("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/api/admin/recurring-reservations/{id}/cancel", recurringReservationId)
                        .with(admin("admin")))
                .andExpect(status().isConflict());
    }

    @Test
    void overlappingRecurringReservationSkipsConflictingOccurrencesInsteadOfFailing() throws Exception {
        createRecurringReservation(FIELD_1, CUSTOMER_A, "WEDNESDAY", "18:00", "19:00");

        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, CUSTOMER_B, "WEDNESDAY", "18:00", "19:00")))
                .andExpect(status().isCreated());

        // Only the first recurringReservation's reservations exist; the second one skipped every occurrence.
        assertThat(activeReservationsFor(FIELD_1)).isEqualTo(5);
    }

    @Test
    void materializeEndpointIsIdempotentAfterCreation() throws Exception {
        createRecurringReservation(FIELD_2, CUSTOMER_A, "THURSDAY", "18:00", "19:00");

        mockMvc.perform(post("/api/admin/recurring-reservations/materialize")
                        .with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(0));

        assertThat(activeReservationsFor(FIELD_2)).isEqualTo(5);
    }

    @Test
    void rejectsRecurringReservationWhoseEndTimeIsNotAfterStartTime() throws Exception {
        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, CUSTOMER_A, "WEDNESDAY", "19:00", "19:00")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void materializesOccurrencesacrossASixMonthWindow() throws Exception {
        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_2, CUSTOMER_B, "THURSDAY", "18:00", "19:00", 6)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.windowMonths").value(6));

        // Every Thursday from 2026-09-03 through 2027-03-01 (6 months): 26 slots
        // (incl. 2026-12-31), far beyond the venue's 1-month public advance cap.
        assertThat(activeReservationsFor(FIELD_2)).isEqualTo(26);
    }

    @Test
    void rejectsRecurringReservationWithUndefinedWindow() throws Exception {
        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, CUSTOMER_A, "WEDNESDAY", "18:00", "19:00", 2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsRecurringReservationWhoseStartIsNotOnTheHalfHourGrid() throws Exception {
        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(FIELD_1, CUSTOMER_A, "WEDNESDAY", "19:15", "20:45")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsRecurringReservationForInactiveResource() throws Exception {
        // Create a dedicated resource so the shared seed-day fields stay untouched.
        String groupId = objectMapper.readTree(mockMvc.perform(get("/api/admin/resource-groups")
                        .with(admin("admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
        String code = "recurring-inactive-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult resourceResult = mockMvc.perform(post("/api/admin/resources")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "%s", "name": "RecurringReservation inactive test field",
                                 "code": "%s", "type": "FOOTBALL_FIELD"}
                                """.formatted(groupId, code)))
                .andExpect(status().isCreated())
                .andReturn();
        String resourceId = objectMapper.readTree(resourceResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/admin/resources/{id}/deactivate", resourceId)
                        .with(admin("admin")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(resourceId, CUSTOMER_A, "WEDNESDAY", "18:00", "19:00")))
                .andExpect(status().isConflict());
    }

    private UUID createRecurringReservation(String resourceId, String customerId, String weekday,
            String startTime, String endTime) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/recurring-reservations")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurringReservationRequest(resourceId, customerId, weekday, startTime, endTime)))
                .andExpect(status().isCreated())
                .andReturn();
        return recurringReservationIdOf(result);
    }

    private UUID recurringReservationIdOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }

    private int activeReservationsFor(UUID recurringReservationId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM reservations
                WHERE recurring_reservation_id = ? AND status = 'ACTIVE'
                """, Integer.class, recurringReservationId);
    }

    private int cancelledReservationsFor(UUID recurringReservationId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM reservations
                WHERE recurring_reservation_id = ? AND status = 'CANCELLED'
                """, Integer.class, recurringReservationId);
    }

    private int activeReservationsFor(String resourceId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM reservations
                WHERE resource_id = ?::uuid AND status = 'ACTIVE'
                """, Integer.class, resourceId);
    }

    private String recurringReservationRequest(String resourceId, String customerId, String weekday,
            String startTime, String endTime) {
        return recurringReservationRequest(resourceId, customerId, weekday, startTime, endTime, 1);
    }

    private String recurringReservationRequest(String resourceId, String customerId, String weekday,
            String startTime, String endTime, int windowMonths) {
        return """
                {"resourceId": "%s", "customerId": "%s", "weekday": "%s",
                 "startTime": "%s", "endTime": "%s", "windowMonths": %d}
                """.formatted(resourceId, customerId, weekday, startTime, endTime, windowMonths);
    }
}