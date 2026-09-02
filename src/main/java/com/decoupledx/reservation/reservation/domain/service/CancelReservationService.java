package com.decoupledx.reservation.reservation.domain.service;

import java.time.Clock;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CancelReservationService {

    private final ReservationRepository reservations;
    private final ResourceService resourceService;
    private final PolicyService policyService;
    private final Clock clock;
    private final TransactionRunner tx;

    public void cancel(ReservationId reservationId, CustomerId customerId) {
        tx.run(() -> {
            Reservation reservation = loadReservation(reservationId);
            if (!reservation.getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }
            CancellationPolicy currentPolicy = policyService.getCancellationPolicy(venueIdOf(reservation));
            reservation.cancel(clock.instant(), currentPolicy);
            reservations.save(reservation);
            log.info("Reservation cancelled id={} customerId={} actor=customer",
                    reservationId.value(), customerId.value());
        });
    }

    public void cancelAdministratively(ReservationId reservationId, CustomerId actor) {
        tx.run(() -> {
            Reservation reservation = loadReservation(reservationId);
            reservation.cancelAdministratively(clock.instant(), actor);
            reservations.save(reservation);
            log.info("Reservation administratively cancelled id={} actor={}",
                    reservationId.value(), actor.value());
        });
    }

    private Reservation loadReservation(ReservationId reservationId) {
        return reservations.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private VenueId venueIdOf(Reservation reservation) {
        return resourceService.getResource(reservation.getResourceId()).venueId();
    }
}
