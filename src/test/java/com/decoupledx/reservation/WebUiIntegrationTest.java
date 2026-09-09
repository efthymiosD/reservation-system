package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * T3 web UI foundation: home page, navigation states, static assets.
 * Error-page HTML rendering is exercised by the real servlet container only
 * (verified live); under MockMvc only the status codes are asserted because
 * Boot's /error dispatch depends on container error-page mechanics.
 * Unknown paths redirect anonymous users to Keycloak by design (until the
 * web UI grows more permitted pages), hence the authenticated 404 check.
 */
@AutoConfigureMockMvc
class WebUiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageIsAccessibleWithoutAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Make a reservation");
        assertThat(html).contains("Log in");
        assertThat(html).doesNotContain("Signed in as");
    }

    @Test
    void homePageShowsSignedInUserWithLogoutWhenAuthenticated() throws Exception {
        MvcResult result = mockMvc.perform(get("/").with(webUser("alice")))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Signed in as alice");
        assertThat(html).contains("Log out");
        assertThat(html).doesNotContain(">Log in<");
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
