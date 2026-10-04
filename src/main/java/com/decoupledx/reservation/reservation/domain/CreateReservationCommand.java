package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import java.time.Instant;
import java.util.Objects;

record CreateReservationCommand(
        ResourceId resourceId,
        Instant start,
        Instant end) {

    CreateReservationCommand {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
    }
}
