package com.decoupledx.reservation.resource.api;

import com.decoupledx.reservation.venue.api.VenueId;

public record ResourceInfo(
        ResourceId id,
        ResourceGroupId groupId,
        VenueId venueId,
        String name,
        String code,
        ResourceType type,
        ResourceStatus status) {

    public boolean isActive() {
        return status == ResourceStatus.ACTIVE;
    }
}
