package com.decoupledx.reservation.venue.adapter.out.persistence;

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
@Table(name = "venues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class VenueEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String address;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    VenueEntity(UUID id, String name, String description, String address, String timezone,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void updateFrom(String name, String description, String address, String timezone, Instant updatedAt) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.timezone = timezone;
        this.updatedAt = updatedAt;
    }
}
