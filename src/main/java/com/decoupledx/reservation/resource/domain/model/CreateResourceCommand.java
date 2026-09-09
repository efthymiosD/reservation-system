package com.decoupledx.reservation.resource.domain.model;

import com.decoupledx.reservation.resource.api.ResourceType;
import com.decoupledx.reservation.resource.api.ResourceGroupId;
import java.util.Objects;

import com.decoupledx.reservation.venue.api.VenueId;

public record CreateResourceCommand(
        VenueId venueId,
        ResourceGroupId groupId,
        String name,
        String code,
        ResourceType type) {

    public CreateResourceCommand {
        Objects.requireNonNull(venueId, "venueId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
