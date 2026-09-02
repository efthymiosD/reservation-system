package com.decoupledx.reservation.reservation.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "reservation id must not be null");
    }

    public static ReservationId of(UUID value) {
        return new ReservationId(value);
    }

    public static ReservationId random() {
        return new ReservationId(UUID.randomUUID());
    }
}
