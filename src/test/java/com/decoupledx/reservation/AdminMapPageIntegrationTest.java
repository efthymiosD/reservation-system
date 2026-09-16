package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

import java.util.UUID;

/**
 * Core-flow /admin/map field-layout editor tests (testing convention): role
 * gating, page reachability, add-field (which persists a placement), layout
 * save via the per-field form inputs, and activate/deactivate. Page internals
 * (drag-and-drop gestures) are verified manually/live, not asserted here.
 */
@AutoConfigureMockMvc
class AdminMapPageIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void mapPageRedirectsAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin/map")).andExpect(status().is3xxRedirection());
    }

    @Test
    void mapPageIsForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/admin/map").with(webUser("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapPageLoadsForAdmins() throws Exception {
        mockMvc.perform(get("/admin/map").with(webAdmin("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAddFieldFromMapPage() throws Exception {
        int before = jdbc.queryForObject(
                "SELECT count(*) FROM resources WHERE code LIKE 'FIELD-%'", Integer.class);

        mockMvc.perform(post("/admin/map/fields")
                        .param("name", "End Zone")
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/map"));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM resources WHERE code LIKE 'FIELD-%'", Integer.class))
                .isEqualTo(before + 1);

        // Shared-container discipline (AGENTS.md): /admin/map/fields persists an
        // ACTIVE FIELD-% row AND upserts its placement into the shared
        // 'venue.map.layout' JSON. The public availability map asserts 6 seeded
        // FIELD-% resources regardless of class order, so undo both traces
        // before returning (order-independent shared dataset).
        UUID createdId = jdbc.queryForObject(
                "SELECT id FROM resources WHERE name = ? AND code LIKE 'FIELD-%'", UUID.class,
                "End Zone");

        // 1) drop the persisted placement from the shared layout JSON
        String layout = jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'venue.map.layout'", String.class);
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        tools.jackson.databind.node.ObjectNode root =
                (tools.jackson.databind.node.ObjectNode) mapper.readTree(layout);
        tools.jackson.databind.node.ArrayNode placements =
                (tools.jackson.databind.node.ArrayNode) root.get("placements");
        for (int i = placements.size() - 1; i >= 0; i--) {
            if (placements.get(i).path("resourceId").asText().equals(createdId.toString())) {
                placements.remove(i);
            }
        }
        jdbc.update("UPDATE site_content SET body = ? WHERE key = 'venue.map.layout'",
                mapper.writeValueAsString(root));

        // 2) remove the resource row (reservations referencing it, if any)
        jdbc.update("DELETE FROM reservations WHERE resource_id = ?", createdId);
        jdbc.update("DELETE FROM resources WHERE id = ?", createdId);
    }

    @Test
    void adminCanSaveFieldLayoutFromMapPage() throws Exception {
        mockMvc.perform(post("/admin/map/layout")
                        .param("name-" + FIELD_1, "Court A")
                        .param("x-" + FIELD_1, "10")
                        .param("y-" + FIELD_1, "20")
                        .param("width-" + FIELD_1, "220")
                        .param("height-" + FIELD_1, "380")
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/map"));

        assertThat(jdbc.queryForObject(
                "SELECT name FROM resources WHERE id = ?", String.class,
                UUID.fromString(FIELD_1))).isEqualTo("Court A");
        String layout = jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'venue.map.layout'", String.class);
        assertThat(layout).contains("\"resourceId\":\"a0000000-0000-0000-0000-000000000101\"")
                .contains("\"x\":10")
                .contains("\"y\":20")
                .contains("\"width\":220")
                .contains("\"height\":380");
    }

    @Test
    void adminCanToggleFieldActivityFromMapPage() throws Exception {
        mockMvc.perform(post("/admin/map/field/{id}/deactivate", FIELD_1)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/map"));

        assertThat(jdbc.queryForObject(
                "SELECT status FROM resources WHERE id = ?", String.class,
                UUID.fromString(FIELD_1))).isEqualTo("INACTIVE");

        mockMvc.perform(post("/admin/map/field/{id}/activate", FIELD_1)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/map"));

        assertThat(jdbc.queryForObject(
                "SELECT status FROM resources WHERE id = ?", String.class,
                UUID.fromString(FIELD_1))).isEqualTo("ACTIVE");
    }
}