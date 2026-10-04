package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReservationApiConfig {

    @Bean
    ReservationApi reservationApi(CreateReservationService createReservationService,
                                  CancelReservationService cancelReservationService,
                                  ReservationQueryService reservationQueryService) {
        return new ReservationApiImpl(createReservationService, cancelReservationService,
                reservationQueryService);
    }
}
