package com.decoupledx.reservation.resource.domain.model;

import com.decoupledx.reservation.resource.adapter.api.ResourceType;
import com.decoupledx.reservation.resource.adapter.api.ResourceGroupId;
import com.decoupledx.reservation.venue.adapter.api.VenueId;

public record ResourceGroupInfo(
        ResourceGroupId id,
        VenueId venueId,
        String name,
        ResourceType type) {
}
