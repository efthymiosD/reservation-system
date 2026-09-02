package com.decoupledx.reservation.resource.domain.model;

import java.time.Instant;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

public record ResourceBlockInfo(
        ResourceBlockId id,
        ResourceId resourceId,
        ReservationPeriod period,
        String reason,
        BlockStatus status,
        Instant createdAt,
        Instant cancelledAt,
        CustomerId cancelledBy) {

    public boolean isActive() {
        return status == BlockStatus.ACTIVE;
    }
}
