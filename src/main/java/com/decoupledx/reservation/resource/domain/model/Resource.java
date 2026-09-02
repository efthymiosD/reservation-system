package com.decoupledx.reservation.resource.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.Getter;

@Getter
public class Resource {

    private final ResourceId id;
    private final ResourceGroupId groupId;
    private final VenueId venueId;
    private String name;
    private final String code;
    private final ResourceType type;
    private ResourceStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Resource(ResourceId id, ResourceGroupId groupId, VenueId venueId, String name, String code,
                     ResourceType type, ResourceStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.venueId = venueId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Resource create(ResourceGroupId groupId, VenueId venueId, String name, String code,
                                  ResourceType type, Instant now) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(venueId, "venueId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return new Resource(ResourceId.random(), groupId, venueId, name, code, type,
                ResourceStatus.ACTIVE, now, now);
    }

    public static Resource reconstitute(ResourceId id, ResourceGroupId groupId, VenueId venueId, String name,
                                        String code, ResourceType type, ResourceStatus status,
                                        Instant createdAt, Instant updatedAt) {
        return new Resource(id, groupId, venueId, name, code, type, status, createdAt, updatedAt);
    }

    public void activate() {
        this.status = ResourceStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = ResourceStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void rename(String newName) {
        Objects.requireNonNull(newName, "name must not be null");
        if (newName.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == ResourceStatus.ACTIVE;
    }
}
