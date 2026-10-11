package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.shared.DomainEvent;
import java.util.UUID;

/**
 * A recurring reservation was cancelled — the still-active future occurrences
 * materialized from it must be released. The event lives in this module
 * because the reservation module is the reactor (ADR-0004 cycle-break rule):
 * the administration module publishes the fact and this module's listener
 * fails its own linked rows with the carried actor (listeners run after the
 * publishing transaction commits, outside the request's security context).
 */
public record RecurringReservationReleaseRequested(UUID recurringReservationId, String actor)
        implements DomainEvent {
}
