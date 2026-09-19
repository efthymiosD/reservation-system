package com.decoupledx.reservation.administration.adapter.in.scheduling;

import com.decoupledx.reservation.administration.domain.RecurringReservationMaterializationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the task scheduler and wires the recurring-reservation materializer to it. The
 * scheduled job itself no-ops unless {@code app.recurring-reservation.materialization-enabled}
 * is explicitly set (integration tests leave it unset), so the fixed-clock test
 * data can never be touched by a live schedule.
 */
@Configuration
@EnableScheduling
public class RecurringReservationSchedulingConfig {

    @Bean
    RecurringReservationScheduler recurringReservationScheduler(
            RecurringReservationMaterializationService materializer,
            @Value("${app.recurring-reservation.materialization-enabled:false}") boolean enabled) {
        return new RecurringReservationScheduler(materializer, enabled);
    }
}