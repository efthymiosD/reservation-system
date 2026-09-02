package com.decoupledx.reservation.reservation.domain.model;

import java.time.Instant;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.Money;

public record ReservationInfo(
        ReservationId id,
        ResourceId resourceId,
        CustomerId customerId,
        Instant start,
        Instant end,
        ReservationStatus status,
        Money price,
        Instant createdAt,
        Instant cancelledAt) {

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }
}
