package com.decoupledx.reservation.shared;

/**
 * Inbound contract for event consumers (ADR-0004): each module that reacts to
 * domain events declares ONE of these — its {@code adapter/listeners/} class —
 * and exposes one {@link DomainEventReaction}-annotated method per event type
 * it is interested in. The reactions stay per-method granular for the event
 * publication registry (one row per reaction), while the module surface stays
 * a single, greppable listener class no matter how many events it consumes.
 *
 * <p>Handlers run after the publishing transaction commits, outside the
 * request's security context; they are idempotent by construction
 * (redelivery via registry resubmission must be safe).</p>
 */
public interface DomainEventHandler {
}
