package com.decoupledx.reservation.resource.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.resource.domain.model.BlockStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

import lombok.Getter;

@Getter
public class ResourceBlock {

    private final ResourceBlockId id;
    private final ResourceId resourceId;
    private final ReservationPeriod period;
    private final String reason;
    private BlockStatus status;
    private final Instant createdAt;
    private Instant cancelledAt;
    private CustomerId cancelledBy;

    private ResourceBlock(ResourceBlockId id, ResourceId resourceId, ReservationPeriod period,
                          String reason, BlockStatus status, Instant createdAt,
                          Instant cancelledAt, CustomerId cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.period = period;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    public static ResourceBlock create(ResourceId resourceId, ReservationPeriod period, String reason, Instant now) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return new ResourceBlock(ResourceBlockId.random(), resourceId, period, reason, BlockStatus.ACTIVE, now,
                null, null);
    }

    public static ResourceBlock reconstitute(ResourceBlockId id, ResourceId resourceId, ReservationPeriod period,
                                             String reason, BlockStatus status, Instant createdAt,
                                             Instant cancelledAt, CustomerId cancelledBy) {
        return new ResourceBlock(id, resourceId, period, reason, status, createdAt, cancelledAt, cancelledBy);
    }

    public void cancel(Instant now, CustomerId actor) {
        if (status == BlockStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.BLOCK_ALREADY_CANCELLED);
        }
        this.status = BlockStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = actor;
    }

    public boolean isActive() {
        return status == BlockStatus.ACTIVE;
    }
}
