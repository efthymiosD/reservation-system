package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Black-box admin API coverage for the editable site content and venue profile:
 * site text blocks are readable/updatable by admins only and persist to the DB.
 */
@AutoConfigureMockMvc
class SiteContentAdminApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contentListIsForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/api/admin/content").with(customer("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListSiteContent() throws Exception {
        mockMvc.perform(get("/api/admin/content").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'home.feature.1.title')]").isNotEmpty());
    }

    @Test
    void adminCanUpdateSiteContentBody() throws Exception {
        mockMvc.perform(put("/api/admin/content/home.feature.1.title")
                        .with(admin("admin"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"body\":\"Updated feature title\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'home.feature.1.title'", String.class))
                .isEqualTo("Updated feature title");
    }

    @Test
    void adminCanUpdateVenueProfile() throws Exception {
        mockMvc.perform(get("/api/admin/venue").with(admin("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isString());

        mockMvc.perform(put("/api/admin/venue")
                        .with(admin("admin"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Five-a-Side Football Centre",
                                 "description": "Six floodlit 5x5 fields.",
                                 "address": "New Street 1, 00-000 Wrocław"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject(
                "SELECT address FROM venues WHERE id = 'a0000000-0000-0000-0000-000000000001'",
                String.class)).isEqualTo("New Street 1, 00-000 Wrocław");
    }
}