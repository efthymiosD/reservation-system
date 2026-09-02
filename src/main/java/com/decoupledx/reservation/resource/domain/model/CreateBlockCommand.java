package com.decoupledx.reservation.resource.domain.model;

import java.util.Objects;

import com.decoupledx.reservation.shared.domain.ReservationPeriod;

public record CreateBlockCommand(
        ResourceId resourceId,
        ReservationPeriod period,
        String reason) {

    public CreateBlockCommand {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
