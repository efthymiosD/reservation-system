package com.decoupledx.reservation;

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
 * T4 public pages: about, opening hours, contact — all server-rendered from the
 * venue domain (seeded data), accessible without authentication.
 */
@AutoConfigureMockMvc
class PublicPagesIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeHeroShowsVenueData() throws Exception {
        String html = bodyOf(get("/"));
        assertThat(html).contains("Five-a-Side Football Centre");
        assertThat(html).contains("Six floodlit 5x5 football fields available for hourly booking.");
    }

    @Test
    void aboutPageShowsVenueDescriptionAndAddress() throws Exception {
        String html = bodyOf(get("/about"));
        assertThat(html).contains("Five-a-Side Football Centre");
        assertThat(html).contains("Six floodlit 5x5 football fields");
        assertThat(html).contains("Sportowa 5, 00-001 Warszawa");
        assertThat(html).contains("Europe/Warsaw");
    }

    @Test
    void openingHoursPageShowsWeeklySchedule() throws Exception {
        String html = bodyOf(get("/opening-hours"));
        for (String day : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}) {
            assertThat(html).contains(day);
        }
        assertThat(html).contains("14:00 – 23:00");
    }

    @Test
    void contactPageShowsAddressAndDirections() throws Exception {
        String html = bodyOf(get("/contact"));
        assertThat(html).contains("Sportowa 5, 00-001 Warszawa");
        assertThat(html).contains("Make a reservation");
    }

    private String bodyOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString();
    }
}
