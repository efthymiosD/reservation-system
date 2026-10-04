package com.decoupledx.reservation.policy.adapter.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cancellation_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CancellationPolicyEntity {

    @Id
    @Column(name = "venue_id")
    private UUID venueId;

    @Column(name = "deadline_before_start_minutes", nullable = false)
    private int deadlineBeforeStartMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    CancellationPolicyEntity(UUID venueId, int deadlineBeforeStartMinutes, Instant updatedAt) {
        this.venueId = venueId;
        this.deadlineBeforeStartMinutes = deadlineBeforeStartMinutes;
        this.updatedAt = updatedAt;
    }

    void updateFrom(int deadlineBeforeStartMinutes, Instant updatedAt) {
        this.deadlineBeforeStartMinutes = deadlineBeforeStartMinutes;
        this.updatedAt = updatedAt;
    }
}
