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
}
