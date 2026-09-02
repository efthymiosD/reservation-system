package com.decoupledx.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.decoupledx.reservation.administration.domain.service.BlockResourceService;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.CreateReservationCommand;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.resource.domain.model.CreateBlockCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

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

    @Autowired
    CreateReservationService createReservation;

    @Autowired
    BlockResourceService blockResource;

    @Test
    void onlyOneConcurrentReservationForSameResourceSucceeds() throws Exception {
        ReservationPeriod period = periodOn(20, LocalTime.of(18, 0), Duration.ofMinutes(90));
        ResourceId resourceId = ResourceId.of(java.util.UUID.fromString(FIELD_1));

        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            CustomerId customer = CustomerId.of("racer-resource-" + i);
            attempts.add(() -> attemptReservation(resourceId, period, customer));
        }

        assertThat(countSuccesses(attempts)).isEqualTo(1);
    }

    @Test
    void onlyOneConcurrentReservationPerCustomerSucceeds() throws Exception {
        ReservationPeriod period = periodOn(21, LocalTime.of(18, 0), Duration.ofMinutes(90));
        CustomerId customer = CustomerId.of("racer-customer");

        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (String field : ALL_FIELDS) {
            ResourceId resourceId = ResourceId.of(java.util.UUID.fromString(field));
            attempts.add(() -> attemptReservation(resourceId, period, customer));
        }

        assertThat(countSuccesses(attempts)).isEqualTo(1);
    }

    @RepeatedTest(3)
    void reservationAndBlockRaceIsSerialized(RepetitionInfo repetitionInfo) throws Exception {
        int day = 22 + repetitionInfo.getCurrentRepetition();
        ReservationPeriod reservationPeriod = periodOn(day, LocalTime.of(18, 0), Duration.ofMinutes(60));
        ReservationPeriod blockPeriod = periodOn(day, LocalTime.of(18, 30), Duration.ofMinutes(60));
        ResourceId resourceId = ResourceId.of(java.util.UUID.fromString(FIELD_1));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> reservationAttempt = pool.submit(
                    () -> attemptReservation(resourceId, reservationPeriod, CustomerId.of("race-customer")));
            Future<Boolean> blockAttempt = pool.submit(() -> {
                try {
                    blockResource.block(new CreateBlockCommand(resourceId, blockPeriod, "race block"));
                    return true;
                } catch (BusinessException exception) {
                    return false;
                }
            });

            int successes = (reservationAttempt.get() ? 1 : 0) + (blockAttempt.get() ? 1 : 0);
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean attemptReservation(ResourceId resourceId, ReservationPeriod period, CustomerId customer) {
        try {
            createReservation.create(new CreateReservationCommand(resourceId, period.start(), period.end()),
                    customer);
            return true;
        } catch (BusinessException exception) {
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

    private static ReservationPeriod periodOn(int dayOfMonth, LocalTime start, Duration duration) {
        Instant startTime = LocalDate.of(2026, 9, dayOfMonth).atTime(start).atZone(WARSAW).toInstant();
        return ReservationPeriod.ofStartAndDuration(startTime, duration);
    }
}
