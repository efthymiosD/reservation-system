package com.decoupledx.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Core-flow coverage only (testing convention): the public pages are reachable
 * without authentication. Page internals are not asserted.
 */
@AutoConfigureMockMvc
class PublicPagesIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void aboutPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/about")).andExpect(status().isOk());
    }

    @Test
    void oldOpeningHoursPageRedirectsToContact() throws Exception {
        mockMvc.perform(get("/opening-hours"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> result.getResponse().getRedirectedUrl().endsWith("/contact"));
    }

    @Test
    void contactPageIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/contact")).andExpect(status().isOk());
    }
}
