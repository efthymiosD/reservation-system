package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resource_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ResourceGroupEntity {

    @Id
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    ResourceGroupEntity(UUID id, UUID venueId, String name, String type, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.venueId = venueId;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void updateFrom(String name, Instant updatedAt) {
        this.name = name;
        this.updatedAt = updatedAt;
    }
}
