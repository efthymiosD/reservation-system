package com.decoupledx.reservation.reservation.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.resource.domain.model.ResourceId;

public record CreateReservationCommand(
        ResourceId resourceId,
        Instant start,
        Instant end) {

    public CreateReservationCommand {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
    }
}
