package com.decoupledx.reservation.resource.adapter.api;

import com.decoupledx.reservation.venue.adapter.api.VenueId;

public record ResourceGroupInfo(
        ResourceGroupId id,
        VenueId venueId,
        String name,
        ResourceType type) {
}
