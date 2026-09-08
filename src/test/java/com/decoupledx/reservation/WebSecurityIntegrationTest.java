package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Verifies the dual security chains introduced for the web UI: the API stays a
 * stateless bearer-token resource server, while browser requests use OAuth2 login
 * (authorization code) against Keycloak. No live Keycloak required: the login
 * redirect is only built, never fetched.
 */
@AutoConfigureMockMvc
class WebSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousBrowserRequestRedirectsToKeycloakLogin() throws Exception {
        // Step 1: a protected page (my reservations, T7) redirects to the local
        // authorization endpoint. Public pages incl. /reserve do NOT redirect:
        // browsing the venue map before login is deliberate.
        mockMvc.perform(get("/my-reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/oauth2/authorization/keycloak")));

        // Step 2: the authorization endpoint builds the real Keycloak URL.
        mockMvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString(
                                "http://localhost:8081/realms/reservation/protocol/openid-connect/auth")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("client_id=reservation-web")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("response_type=code")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString(
                                "redirect_uri=http://localhost/login/oauth2/code/keycloak")));
    }

    @Test
    void publicApiRemainsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/venue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Five-a-Side Football Centre"));
    }

    @Test
    void apiStillRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiStillAcceptsBearerTokens() throws Exception {
        mockMvc.perform(get("/api/reservations").with(customer("user-1")))
                .andExpect(status().isOk());
    }
}
