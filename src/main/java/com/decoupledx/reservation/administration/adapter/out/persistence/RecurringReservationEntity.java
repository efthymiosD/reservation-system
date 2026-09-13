package com.decoupledx.reservation.administration.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
@Table(name = "recurring_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecurringReservationEntity {

    @Id
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String status;

    @Column(name = "next_occurrence", nullable = false)
    private LocalDate nextOccurrence;

    @Column(name = "window_months", nullable = false)
    private Integer windowMonths;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Version
    private long version;

    RecurringReservationEntity(UUID id, UUID resourceId, UUID customerId, String weekday,
                              LocalTime startTime, LocalTime endTime, int windowMonths, String status,
                              LocalDate nextOccurrence, Instant createdAt, Instant cancelledAt, String cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.customerId = customerId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.windowMonths = windowMonths;
        this.status = status;
        this.nextOccurrence = nextOccurrence;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    void updateFrom(String status, LocalDate nextOccurrence, Instant cancelledAt, String cancelledBy) {
        this.status = status;
        this.nextOccurrence = nextOccurrence;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }
}