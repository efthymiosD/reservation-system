package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Core-flow coverage only (testing convention): the home page is reachable, the
 * static assets are served, and the error paths return the right statuses.
 * Page internals are not asserted. Unknown paths redirect anonymous users to
 * Keycloak by design (until the web UI grows more permitted pages), hence the
 * authenticated 404 check.
 */
@AutoConfigureMockMvc
class WebUiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void homePageIsAccessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/").with(webUser("alice"))).andExpect(status().isOk());
    }

    @Test
    void staticAssetsAreServedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/vendor/bootstrap.min.css")).andExpect(status().isOk());
        mockMvc.perform(get("/css/site.css")).andExpect(status().isOk());
    }

    @Test
    void unknownPageReturnsNotFoundForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/nonexistent-page").with(webUser("alice")))
                .andExpect(status().isNotFound());
    }

    @Test
    void csrflessPostIsRejectedWith403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/")
                        .with(webUser("alice")))
                .andExpect(status().isForbidden());
    }
}
