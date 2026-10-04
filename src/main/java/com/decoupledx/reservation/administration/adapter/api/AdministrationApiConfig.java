package com.decoupledx.reservation.administration.adapter.api;

import com.decoupledx.reservation.administration.domain.port.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AdministrationApiConfig {

    @Bean
    AdministrationApi administrationApi(RecurringReservationService recurringReservationService,
                                        RecurringReservationMaterializationService materializationService) {
        return new AdministrationApiImpl(recurringReservationService, materializationService);
    }
}