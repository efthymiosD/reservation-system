package com.decoupledx.reservation.resource.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence view of a resource.
 */
public record ResourceDataValue(UUID id, UUID groupId, UUID venueId, String name, String code,
                                String type, String status, Instant createdAt, Instant updatedAt) {
}
