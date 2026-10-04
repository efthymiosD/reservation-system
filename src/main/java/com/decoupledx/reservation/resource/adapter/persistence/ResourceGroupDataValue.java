package com.decoupledx.reservation.resource.adapter.persistence;

import java.util.UUID;

/**
 * Persistence view of a resource group.
 */
public record ResourceGroupDataValue(UUID id, UUID venueId, String name, String type) {
}
