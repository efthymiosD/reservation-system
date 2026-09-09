package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Late-evening clock (23:30 venue-local, after the 14:00-23:00 opening day):
 * every start time for "today" is in the past, so the reservation page must
 * automatically fall forward to the next bookable day and say so.
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
    void exhaustedDayAutomaticallyFallsForwardToNextBookableDay() throws Exception {
        MvcResult result = mockMvc.perform(get("/reserve").with(webUser("alice")))
                .andExpect(status().isOk())
                .andReturn();
        String html = flat(result.getResponse().getContentAsString());

        assertThat(html).contains("2026-09-02");
        assertThat(html).doesNotContain("value=\"2026-09-01\"");
        assertThat(html).contains("There are no bookable start times on the selected day");
        assertThat(html).contains("showing 2026-09-02 instead");
        // The next day's options start at opening time.
        assertThat(html).contains(">14:00<");
    }

    private static String flat(String html) {
        return html.replaceAll("\\s+", " ");
    }
}
