package com.decoupledx.reservation.shared;

/**
 * Marker for domain events: immutable, business-meaningful facts emitted by a
 * module after one of its aggregate state changes. Payloads carry resolved
 * application data — including the acting {@code CustomerId} where the
 * originating use case had one — because event listeners run after the
 * publishing transaction commits, outside the request's security context.
 *
 * <p>Implementations live in the publishing module's {@code adapter/api}
 * package and are plain records (ADR-0003/0004).</p>
 */
public interface DomainEvent {
}
