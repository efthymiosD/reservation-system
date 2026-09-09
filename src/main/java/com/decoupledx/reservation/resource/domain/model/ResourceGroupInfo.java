package com.decoupledx.reservation.resource.domain.model;

import com.decoupledx.reservation.resource.api.ResourceType;
import com.decoupledx.reservation.resource.api.ResourceGroupId;
import com.decoupledx.reservation.venue.api.VenueId;

public record ResourceGroupInfo(
        ResourceGroupId id,
        VenueId venueId,
        String name,
        ResourceType type) {
}
