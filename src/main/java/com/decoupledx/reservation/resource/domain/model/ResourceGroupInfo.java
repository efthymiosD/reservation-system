package com.decoupledx.reservation.resource.domain.model;

import com.decoupledx.reservation.venue.domain.model.VenueId;

public record ResourceGroupInfo(
        ResourceGroupId id,
        VenueId venueId,
        String name,
        ResourceType type) {
}
