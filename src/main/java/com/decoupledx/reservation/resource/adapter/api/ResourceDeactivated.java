package com.decoupledx.reservation.resource.adapter.api;

import com.decoupledx.reservation.shared.DomainEvent;
import java.util.UUID;

/**
 * A resource (field) was deactivated. Published informationally in this
 * slice: no consumer reacts destructively (existing bookings stand; the
 * deactivation makes the field unbookable via RESOURCE_INACTIVE checks).
 */
public record ResourceDeactivated(UUID resourceId) implements DomainEvent {
}
