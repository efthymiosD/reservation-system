package com.decoupledx.reservation.reservation.adapter.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BookingRequestEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String status;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false)
    @Setter
    private String contact;

    @Column
    @Setter
    private String note;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "submission_key", nullable = false, unique = true)
    private String submissionKey;

    @Column(name = "advisory_free_at_submission", nullable = false)
    private boolean advisoryFreeAtSubmission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "decided_at")
    @Setter
    private Instant decidedAt;

    @Column(name = "decided_by")
    @Setter
    private String decidedBy;

    @Column(name = "reservation_id")
    @Setter
    private UUID reservationId;

    @Version
    private long version;

    BookingRequestEntity(UUID id, String status, UUID resourceId,
                         LocalDateTime startAt, int durationMinutes, LocalDateTime endAt,
                         String customerName, String contact, String note,
                         UUID linkId, String submissionKey, boolean advisoryFreeAtSubmission,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.resourceId = resourceId;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.endAt = endAt;
        this.customerName = customerName;
        this.contact = contact;
        this.note = note;
        this.linkId = linkId;
        this.submissionKey = submissionKey;
        this.advisoryFreeAtSubmission = advisoryFreeAtSubmission;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Applies a resolved decision (status + dispose fields — guarded by the
     * domain model's transition checks; the version column still protects
     * against concurrent decisions).
     */
    void applyDecision(String status, Instant decidedAt, String decidedBy,
                       UUID reservationId, Instant updatedAt) {
        this.status = status;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
        this.reservationId = reservationId;
        this.updatedAt = updatedAt;
    }
}
