package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import java.util.UUID;

/**
 * Inbound port: cancellation use cases (customer cancel + administrative cancel).
 */
public interface CancelReservationService {

    /**
     * Customer-facing entry point for the current player (ADR 0002); ownership
     * is checked against the customer resolved from the session context.
     */
    void cancel(UUID reservationId);

    void cancel(UUID reservationId, CustomerId customer);

    /**
     * Administrative cancel performed by the current player acting as
     * administrator (ADR 0002); the actor is resolved from the session context.
     */
    void cancelAdministratively(UUID reservationId);

    void cancelAdministratively(UUID reservationId, CustomerId actor);

    /**
     * Releases the still-active future occurrences a recurring reservation
     * materialized, cancelling each with the given actor (the recurring
     * cancellation's administrator). The past-skipping and already-cancelled
     * tolerance are release policy of this use case — callers, e.g. the
     * module's event listener, stay logic-free.
     */
    void releaseFutureOccurrencesOf(UUID recurringReservationId, CustomerId actor);
}
