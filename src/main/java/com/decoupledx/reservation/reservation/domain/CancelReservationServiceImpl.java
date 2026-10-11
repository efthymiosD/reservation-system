package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationCancelled;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.DomainEventPublisher;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class CancelReservationServiceImpl implements CancelReservationService {

    private final ReservationRepository reservations;
    private final CurrentCustomerApi currentCustomers;
    private final ResourceApi resourceService;
    private final PolicyApi policyService;
    private final Clock clock;
    private final TransactionRunner tx;
    private final DomainEventPublisher events;

    @Override
    public void cancel(UUID reservationId) {
        cancel(reservationId, currentCustomers.currentCustomerId());
    }

    @Override
    public void cancel(UUID reservationId, CustomerId customerId) {
        tx.run(() -> {
            Reservation reservation = loadReservation(ReservationId.of(reservationId));
            if (!reservation.getCustomerId().equals(customerId)) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }
            CancellationPolicy currentPolicy = policyService.cancellationPolicyFor(venueIdOf(reservation));
            reservation.cancel(clock.instant(), currentPolicy);
            reservations.save(reservation.toDataValue());
            publishCancelled(reservation);
            log.info("Reservation cancelled id={} customerId={} actor=customer",
                    reservationId, customerId.value());
        });
    }

    @Override
    public void cancelAdministratively(UUID reservationId) {
        cancelAdministratively(reservationId, currentCustomers.currentCustomerId());
    }

    @Override
    public void cancelAdministratively(UUID reservationId, CustomerId actor) {
        tx.run(() -> {
            Reservation reservation = loadReservation(ReservationId.of(reservationId));
            reservation.cancelAdministratively(clock.instant(), actor);
            reservations.save(reservation.toDataValue());
            publishCancelled(reservation);
            log.info("Reservation administratively cancelled id={} actor={}",
                    reservationId, actor.value());
        });
    }

    @Override
    public void releaseFutureOccurrencesOf(UUID recurringReservationId, CustomerId actor) {
        tx.run(() -> {
            Instant now = clock.instant();
            // Already-cancelled and past occurrences are skipped: registry
            // redelivery of the release event must be a harmless no-op.
            long released = reservations.findActiveByRecurringReservationId(recurringReservationId).stream()
                    .filter(data -> data.endTime().isAfter(now))
                    .map(Reservation::from)
                    .filter(Reservation::isActive)
                    .map(value -> {
                        value.cancelAdministratively(now, actor);
                        return reservations.save(value.toDataValue());
                    })
                    .count();
            log.info("Recurring reservation {} released: {} future occurrence(s) by actor={}",
                    recurringReservationId, released, actor.value());
        });
    }

    private void publishCancelled(Reservation reservation) {
        events.publish(new ReservationCancelled(
                reservation.getId().value(),
                reservation.getResourceId().value(),
                reservation.getCustomerId().value(),
                reservation.getCancelledBy() != null ? reservation.getCancelledBy().value() : null,
                reservation.getCancelledAt(),
                reservation.getRecurringReservationId()));
    }

    private Reservation loadReservation(ReservationId reservationId) {
        return reservations.findById(reservationId)
                .map(Reservation::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private UUID venueIdOf(Reservation reservation) {
        return resourceService.getResource(reservation.getResourceId().value()).venueId().value();
    }
}
