package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Core-flow smoke (testing convention): with a late-evening clock (23:30
 * venue-local, after opening hours) the reservation page still loads for a
 * signed-in user. Page internals are not asserted.
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.clock.fixed-instant=2026-09-01T21:30:00Z",
        "app.security.issuer-uri="
})
class LateNightReservationPageTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reservePageLoadsLateAtNight() throws Exception {
        mockMvc.perform(get("/reserve").with(webUser("alice")))
                .andExpect(status().isOk());
    }
}
