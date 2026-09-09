package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Black-box concurrency tests: every racer goes through the HTTP API
 * (reservations as customers, blocks as admin) exactly like real clients, and
 * the database exclusion constraints decide the winners.
 */
@AutoConfigureMockMvc
class ConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final List<String> ALL_FIELDS = List.of(
            "a0000000-0000-0000-0000-000000000101",
            "a0000000-0000-0000-0000-000000000102",
            "a0000000-0000-0000-0000-000000000103",
            "a0000000-0000-0000-0000-000000000104",
            "a0000000-0000-0000-0000-000000000105",
            "a0000000-0000-0000-0000-000000000106");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void onlyOneConcurrentReservationForSameResourceSucceeds() throws Exception {
        assertThat(countSuccesses(raceReservations(FIELD_1, 20, LocalTime.of(18, 0), 90, 8))).isEqualTo(1);
    }

    @Test
    void onlyOneConcurrentReservationPerCustomerSucceeds() throws Exception {
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < ALL_FIELDS.size(); i++) {
            String field = ALL_FIELDS.get(i);
            attempts.add(() -> attemptReservation(field, 21, LocalTime.of(18, 0), 90,
                    com.decoupledx.reservation.testinfra.JwtSupport.customer("racer-customer")));
        }
        assertThat(countSuccesses(attempts)).isEqualTo(1);
    }

    @RepeatedTest(3)
    void reservationAndBlockRaceIsSerialized(RepetitionInfo repetitionInfo) throws Exception {
        int day = 22 + repetitionInfo.getCurrentRepetition();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> reservationAttempt = pool.submit(() -> attemptReservation(FIELD_1, day,
                    LocalTime.of(18, 0), 60, com.decoupledx.reservation.testinfra.JwtSupport.customer("race-customer")));
            Future<Boolean> blockAttempt = pool.submit(() -> attemptBlock(FIELD_1, day,
                    LocalTime.of(18, 30), 60));

            int successes = (reservationAttempt.get() ? 1 : 0) + (blockAttempt.get() ? 1 : 0);
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Callable<Boolean>> raceReservations(String resourceId, int day, LocalTime start,
            int durationMinutes, int racers) {
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < racers; i++) {
            int racer = i;
            attempts.add(() -> attemptReservation(resourceId, day, start, durationMinutes,
                    com.decoupledx.reservation.testinfra.JwtSupport.customer("racer-resource-" + racer)));
        }
        return attempts;
    }

    /** True when the HTTP attempt won (201), false on a clean conflict (409). */
    private boolean attemptReservation(String resourceId, int day, LocalTime start,
            int durationMinutes, org.springframework.test.web.servlet.request.RequestPostProcessor principal) {
        String body = """
                {"resourceId": "%s", "startTime": "2026-09-%02dT%s", "durationMinutes": %d}
                """.formatted(resourceId, day, TIME_FORMAT.format(start), durationMinutes);
        return post("/api/reservations", body, principal);
    }

    private boolean attemptBlock(String resourceId, int day, LocalTime start, int durationMinutes) {
        String body = """
                {"resourceId": "%s",
                 "startTime": "2026-09-%02dT%s",
                 "endTime": "2026-09-%02dT%s",
                 "reason": "race block"}
                """.formatted(resourceId, day, TIME_FORMAT.format(start),
                day, TIME_FORMAT.format(start.plusMinutes(durationMinutes)));
        return post("/api/admin/resource-blocks", body, admin("admin"));
    }

    private boolean post(String url, String body,
            org.springframework.test.web.servlet.request.RequestPostProcessor principal) {
        try {
            MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post(url)
                            .with(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();
            int status = result.getResponse().getStatus();
            System.out.println("POST-STATUS: " + status);
            System.out.println("POST-BODY: " + body);
            return status == 201 || status == 200;
        } catch (Exception exception) {
            System.out.println("POST-EXCEPTION: " + exception);
            return false;
        }
    }

    private int countSuccesses(List<Callable<Boolean>> attempts) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(attempts.size());
        try {
            int successes = 0;
            for (Future<Boolean> future : pool.invokeAll(attempts)) {
                if (future.get()) {
                    successes++;
                }
            }
            return successes;
        } finally {
            pool.shutdownNow();
        }
    }
}
