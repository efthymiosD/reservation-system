package com.decoupledx.reservation.policy.adapter.out.persistence;

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
@Table(name = "booking_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BookingPolicyEntity {

    @Id
    @Column(name = "venue_id")
    private UUID venueId;

    @Column(name = "min_duration_minutes", nullable = false)
    private int minDurationMinutes;

    @Column(name = "max_duration_minutes", nullable = false)
    private int maxDurationMinutes;

    @Column(name = "duration_step_minutes", nullable = false)
    private int durationStepMinutes;

    @Column(name = "start_time_step_minutes", nullable = false)
    private int startTimeStepMinutes;

    @Column(name = "max_advance_booking", nullable = false)
    private String maxAdvanceBooking;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    BookingPolicyEntity(UUID venueId, int minDurationMinutes, int maxDurationMinutes,
                        int durationStepMinutes, int startTimeStepMinutes, String maxAdvanceBooking,
                        Instant updatedAt) {
        this.venueId = venueId;
        this.minDurationMinutes = minDurationMinutes;
        this.maxDurationMinutes = maxDurationMinutes;
        this.durationStepMinutes = durationStepMinutes;
        this.startTimeStepMinutes = startTimeStepMinutes;
        this.maxAdvanceBooking = maxAdvanceBooking;
        this.updatedAt = updatedAt;
    }

    void updateFrom(int minDurationMinutes, int maxDurationMinutes, int durationStepMinutes,
                    int startTimeStepMinutes, String maxAdvanceBooking, Instant updatedAt) {
        this.minDurationMinutes = minDurationMinutes;
        this.maxDurationMinutes = maxDurationMinutes;
        this.durationStepMinutes = durationStepMinutes;
        this.startTimeStepMinutes = startTimeStepMinutes;
        this.maxAdvanceBooking = maxAdvanceBooking;
        this.updatedAt = updatedAt;
    }
}
