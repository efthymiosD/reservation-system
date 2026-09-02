package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "opening_hours")
@IdClass(OpeningHoursEntityId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OpeningHoursEntity {

    @Id
    @Column(name = "venue_id")
    private UUID venueId;

    @Id
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    OpeningHoursEntity(UUID venueId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.venueId = venueId;
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }
}
