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
@Table(name = "resource_blocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ResourceBlockEntity {

    @Id
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Version
    private long version;

    ResourceBlockEntity(UUID id, UUID resourceId, Instant startTime, Instant endTime, String reason,
                        String status, Instant createdAt, Instant cancelledAt, String cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    void updateFrom(String status, Instant cancelledAt, String cancelledBy) {
        this.status = status;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }
}
