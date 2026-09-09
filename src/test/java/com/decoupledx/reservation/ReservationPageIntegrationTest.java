package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * T5/T6 reservation page (server side): the page is authenticated-only, controls
 * are shaped from backend configuration (durations, grid, advance window), the
 * SVG map renders every resource with its status from the availability service,
 * and the price is backend-computed.
 */
@AutoConfigureMockMvc
class ReservationPageIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final ZoneId VENUE_ZONE = ZoneId.of("Europe/Warsaw");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void pageRendersVenueMapWithAllFieldsForAuthenticatedUsers() throws Exception {
        MvcResult result = mockMvc.perform(get("/reserve").with(webUser("alice")))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Make a reservation");
        assertThat(html).contains("venue-map__svg");
        for (int i = 1; i <= 6; i++) {
            assertThat(html).contains("Field " + i);
        }
        // Default slot: fixed clock 2026-09-01T10:00Z (12:00 local), venue opens 14:00.
        assertThat(html).contains("2026-09-01");
        assertThat(html).contains("18:00"); // default start = first option >= now
        assertThat(html).contains("80.00");
        assertThat(html).contains("PLN");
        assertThat(html).contains("resource--available");
    }

    @Test
    void controlOptionsComeFromBackendConfiguration() throws Exception {
        String html = bodyOf(get("/reserve").with(webUser("alice")));

        // Duration options from the booking policy: 60..120 by 30.
        assertThat(html).contains("value=\"60\"");
        assertThat(html).contains("value=\"90\"");
        assertThat(html).contains("value=\"120\"");
        assertThat(html).doesNotContain("value=\"150\"");

        // 60 min on a 14:00-23:00 day: starts 14:00..22:00 on the 30-min grid,
        // past times (now = 12:00 local) excluded — no option before opening.
        assertThat(html).contains(">14:00<");
        assertThat(html).contains(">22:00<");
        assertThat(html).doesNotContain(">22:30<");
        assertThat(html).doesNotContain(">13:30<");
    }

    @Test
    void timeOptionsRespectTheSelectedDuration() throws Exception {
        String html = flat(bodyOf(get("/reserve?date=2026-09-03&start=18:00&durationMinutes=90").with(webUser("alice"))));

        assertThat(html).contains("2026-09-03");
        assertThat(html).contains("value=\"18:00\" selected=\"selected\"");
        assertThat(html).contains("value=\"90\" selected=\"selected\"");
        // 90 min: latest start is 21:30 (21:30 + 90 min = 23:00 close).
        assertThat(html).contains(">21:30<");
        assertThat(html).doesNotContain(">22:00<");
        assertThat(html).contains("120.00");
    }

    @Test
    void reservedAndBlockedFieldsRenderTheirStates() throws Exception {
        String reservation = """
                {"resourceId": "%s", "startTime": "2026-09-01T18:00:00", "durationMinutes": 60}
                """.formatted(FIELD_1);
        mockMvc.perform(post("/api/reservations").with(customer("map-user"))
                        .contentType(APPLICATION_JSON).content(reservation))
                .andExpect(status().isCreated());
        insertBlock(UUID.fromString(FIELD_2), venueTime(18, 0), venueTime(19, 0));

        String html = bodyOf(get("/reserve?date=2026-09-01&start=18:00&durationMinutes=60")
                .with(webUser("alice")));

        assertThat(html).contains("resource--reserved");
        assertThat(html).contains("Field 1 — reserved — unavailable");
        assertThat(html).contains("resource--blocked");
        assertThat(html).contains("Field 2 — blocked — unavailable");
        assertThat(html).contains("aria-disabled=\"true\"");
    }

    @Test
    void outOfWindowDateFallsBackToTodayWithMessage() throws Exception {
        String html = bodyOf(get("/reserve?date=2027-12-12").with(webUser("alice")));

        assertThat(html).contains("2026-09-01");
        assertThat(html).contains("outside the booking window");
    }

    @Test
    void dateInputIsLimitedByAdvanceWindow() throws Exception {
        String html = bodyOf(get("/reserve").with(webUser("alice")));

        assertThat(html).contains("min=\"2026-09-01\"");
        assertThat(html).contains("max=\"2026-10-01\"");  // today + P1M
    }

    private String bodyOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString();
    }

    /** Collapses whitespace so assertions don't depend on template line breaks. */
    private static String flat(String html) {
        return html.replaceAll("\\s+", " ");
    }

    private void insertBlock(UUID resourceId, OffsetDateTime start, OffsetDateTime end) {
        jdbc.update("""
                INSERT INTO resource_blocks
                    (id, resource_id, start_time, end_time, reason, status, created_at, version)
                VALUES (?, ?, ?, ?, 'maintenance', 'ACTIVE', now(), 0)
                """, UUID.randomUUID(), resourceId, start, end);
    }

    private OffsetDateTime venueTime(int hour, int minute) {
        return LocalDate.of(2026, 9, 1).atTime(hour, minute).atZone(VENUE_ZONE).toOffsetDateTime();
    }
}
