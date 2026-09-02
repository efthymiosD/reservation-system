package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class BlockApiIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final String FIELD_3 = "a0000000-0000-0000-0000-000000000103";
    private static final String FIELD_4 = "a0000000-0000-0000-0000-000000000104";
    private static final String FIELD_5 = "a0000000-0000-0000-0000-000000000105";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void blockEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(customer("regular-user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_1, "2026-09-04T16:00:00", "2026-09-04T18:00:00", "maintenance")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanBlockAFreeSlot() throws Exception {
        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_1, "2026-09-04T16:00:00", "2026-09-04T18:00:00", "maintenance")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("maintenance"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void normalBlockIsRejectedWhenConflictingWithReservation() throws Exception {
        createReservation("block-user-1", FIELD_2, "2026-09-04T18:00:00", 90);

        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_2, "2026-09-04T19:00:00", "2026-09-04T20:00:00", "maintenance")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("RESOURCE_BLOCK_CONFLICT"));
    }

    @Test
    void overrideCancelsConflictingReservationsAtomically() throws Exception {
        String reservationId = createReservation("override-user", FIELD_3, "2026-09-04T18:00:00", 90);

        mockMvc.perform(post("/api/admin/resource-blocks/override")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_3, "2026-09-04T18:30:00", "2026-09-04T20:00:00", "tournament setup")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cancelledReservations").value(1))
                .andExpect(jsonPath("$.block.status").value("ACTIVE"));

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .with(customer("override-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void blockedResourceIsExcludedFromAvailability() throws Exception {
        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_4, "2026-09-05T18:00:00", "2026-09-05T20:00:00", "repair")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/availability")
                        .with(customer("availability-user"))
                        .param("date", "2026-09-05")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resourceId == '" + FIELD_4 + "')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.resourceId == '" + FIELD_1 + "')]").exists());
    }

    @Test
    void overlappingBlocksOnSameResourceAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_5, "2026-09-06T18:00:00", "2026-09-06T20:00:00", "first")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_5, "2026-09-06T19:00:00", "2026-09-06T21:00:00", "second")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("BLOCK_OVERLAPS"));
    }

    @Test
    void cancelledBlockFreesTheSlotAgain() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/resource-blocks")
                        .with(admin("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockRequest(FIELD_1, "2026-09-07T18:00:00", "2026-09-07T20:00:00", "temporary")))
                .andExpect(status().isCreated())
                .andReturn();
        String blockId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/admin/resource-blocks/{id}/cancel", blockId)
                        .with(admin("admin")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/availability")
                        .with(customer("availability-user-2"))
                        .param("date", "2026-09-07")
                        .param("start", "18:00")
                        .param("durationMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resourceId == '" + FIELD_1 + "')]").exists());
    }

    private String createReservation(String subject, String resourceId, String startTime, int minutes)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": "%s", "startTime": "%s", "durationMinutes": %d}
                                """.formatted(resourceId, startTime, minutes)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String blockRequest(String resourceId, String startTime, String endTime, String reason) {
        return """
                {"resourceId": "%s", "startTime": "%s", "endTime": "%s", "reason": "%s"}
                """.formatted(resourceId, startTime, endTime, reason);
    }
}
