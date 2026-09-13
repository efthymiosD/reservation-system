package com.decoupledx.reservation.administration.adapter.in.scheduling;

import com.decoupledx.reservation.administration.domain.service.RecurringReservationMaterializationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Periodic materialization of upcoming recurring-reservation occurrences. Runs only when
 * {@code app.recurring-reservation.materialization-enabled=true} (default in
 * {@code application.yml}; integration tests disable it so the fixed-clock test
 * data is never disturbed by a live schedule).
 */
@Slf4j
@RequiredArgsConstructor
public class RecurringReservationScheduler {

    private final RecurringReservationMaterializationService materializer;
    private final boolean enabled;

    @Scheduled(cron = "${app.recurring-reservation.materialization-cron:0 0 4 * * *}")
    public void materializeDue() {
        if (!enabled) {
            return;
        }
        log.info("Scheduled recurring-reservation materialization run started");
        materializer.materializeDue();
        log.info("Scheduled recurring-reservation materialization run finished");
    }
}