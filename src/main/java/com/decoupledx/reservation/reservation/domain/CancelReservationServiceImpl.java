package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.DomainEventPublisher;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
class CancelReservationServiceImpl implements CancelReservationService {

    private final ReservationRepository dataRepo;
    private final CurrentCustomerApi customerApi;
    private final ResourceApi resourceApi;
    private final PolicyApi policyApi;
    private final Clock clock;
    private final TransactionRunner tx;
    private final DomainEventPublisher eventPublisher;

    @Override
    public void cancel(UUID reservationId) {
        cancel(reservationId, customerApi.currentCustomerId());
    }

    @Override
    public void cancel(UUID reservationId, CustomerId customerId) {
        tx.run(() -> {
            Reservation reservation = loadReservation(ReservationId.of(reservationId));
            if (!reservation.getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }
            CancellationPolicy currentPolicy = policyApi.cancellationPolicyFor(venueIdOf(reservation));
            reservation.cancel(clock.instant(), currentPolicy);
            dataRepo.save(reservation.toDataValue());
            eventPublisher.publish(reservation.toCancelledEvent());
            log.info("Reservation cancelled id={} customerId={} actor=customer", reservationId, customerId.value());
        });
    }

    @Override
    public void cancelAdministratively(UUID reservationId) {
        cancelAdministratively(reservationId, customerApi.currentCustomerId());
    }

    @Override
    public void cancelAdministratively(UUID reservationId, CustomerId actor) {
        tx.run(() -> {
            Reservation reservation = loadReservation(ReservationId.of(reservationId));
            reservation.cancelAdministratively(clock.instant(), actor);
            dataRepo.save(reservation.toDataValue());
            eventPublisher.publish(reservation.toCancelledEvent());
            log.info("Reservation administratively cancelled id={} actor={}", reservationId, actor.value());
        });
    }

    @Override
    public void releaseFutureOccurrencesOf(UUID recurringReservationId, CustomerId actor) {
        tx.run(() -> {
            Instant now = clock.instant();
            // Already-cancelled and past occurrences are skipped: registry
            // redelivery of the release event must be a harmless no-op.
            List<Reservation> activeRecurringReservations = getActiveRecurringReservations(recurringReservationId, now);
            activeRecurringReservations
                    .forEach(reservation -> {
                        reservation.cancelAdministratively(now, actor);
                        dataRepo.save(reservation.toDataValue());
                    });
            log.info("Recurring reservation {} released: {} future occurrence(s) by actor={}",
                    recurringReservationId, activeRecurringReservations.size(), actor.value());
        });
    }

    private List<Reservation> getActiveRecurringReservations(UUID recurringReservationId, Instant now) {
        return dataRepo.findActiveByRecurringReservationId(recurringReservationId).stream()
                .filter(data -> data.endTime().isAfter(now))
                .map(Reservation::from)
                .filter(Reservation::isActive)
                .toList();
    }

    private Reservation loadReservation(ReservationId reservationId) {
        return dataRepo.findById(reservationId)
                .map(Reservation::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private UUID venueIdOf(Reservation reservation) {
        return resourceApi.getResource(reservation.getResourceId().value()).venueId().value();
    }
}
