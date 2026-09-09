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
 * Core-flow coverage only (testing convention): the reservation page is
 * authenticated-only and loads for signed-in users. Page internals are not
 * asserted.
 */
@AutoConfigureMockMvc
class ReservationPageIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reservePageLoadsForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/reserve").with(webUser("alice")))
                .andExpect(status().isOk());
    }

    @Test
    void reservePageRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/reserve"))
                .andExpect(status().is3xxRedirection());
    }
}
