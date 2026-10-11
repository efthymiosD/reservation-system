package com.decoupledx.reservation.reservation.adapter.listeners;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.RecurringReservationReleaseRequested;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.shared.DomainEventHandler;
import com.decoupledx.reservation.shared.DomainEventReaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single place where this module reacts to domain events (ADR-0004):
 * methods translate the event into a use-case call — all reaction policy
 * lives in the domain service; listeners stay logic-free except for mapping
 * the payload into its arguments.
 */
@Component
@RequiredArgsConstructor
class ReservationEventListener implements DomainEventHandler {

    private final CancelReservationService cancels;

    @DomainEventReaction
    void on(RecurringReservationReleaseRequested event) {
        cancels.releaseFutureOccurrencesOf(event.recurringReservationId(), CustomerId.of(event.actor()));
    }
}

