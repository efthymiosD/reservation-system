package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "resources")
class ResourceEntity {

    @Id
    private UUID id;

    @Column(name = "resource_group_id", nullable = false)
    private UUID resourceGroupId;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ResourceEntity() {
    }

    ResourceEntity(UUID id, UUID resourceGroupId, UUID venueId, String name, String code, String type,
                   String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.resourceGroupId = resourceGroupId;
        this.venueId = venueId;
        this.name = name;
        this.code = code;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void updateFrom(String name, String status, Instant updatedAt) {
        this.name = name;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getResourceGroupId() {
        return resourceGroupId;
    }

    UUID getVenueId() {
        return venueId;
    }

    String getName() {
        return name;
    }

    String getCode() {
        return code;
    }

    String getType() {
        return type;
    }

    String getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
