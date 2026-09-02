package com.decoupledx.reservation.resource.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.Getter;

@Getter
public class ResourceGroup {

    private final ResourceGroupId id;
    private final VenueId venueId;
    private String name;
    private final ResourceType type;
    private final Instant createdAt;
    private Instant updatedAt;

    private ResourceGroup(ResourceGroupId id, VenueId venueId, String name, ResourceType type,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.venueId = venueId;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ResourceGroup create(VenueId venueId, String name, ResourceType type, Instant now) {
        Objects.requireNonNull(venueId, "venueId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return new ResourceGroup(ResourceGroupId.random(), venueId, name, type, now, now);
    }

    public static ResourceGroup reconstitute(ResourceGroupId id, VenueId venueId, String name,
                                             ResourceType type, Instant createdAt, Instant updatedAt) {
        return new ResourceGroup(id, venueId, name, type, createdAt, updatedAt);
    }
}
