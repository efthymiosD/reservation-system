package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * A reservation was cancelled. Published for every cancel flow (customer,
 * administrative, recurring-reservation cascade) — one event per actual
 * cancel. The actor is explicit because listeners run outside the request's
 * security context.
 */
public record ReservationCancelled(
        UUID reservationId,
        UUID resourceId,
        String customerId,
        String cancelledBy,
        Instant cancelledAt,
        UUID recurringReservationId) implements DomainEvent {
}
