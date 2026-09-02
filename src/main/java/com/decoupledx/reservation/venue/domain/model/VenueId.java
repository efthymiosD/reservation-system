package com.decoupledx.reservation.venue.domain.model;

import java.util.Objects;
import java.util.UUID;

public record VenueId(UUID value) {

    public VenueId {
        Objects.requireNonNull(value, "venue id must not be null");
    }

    public static VenueId of(UUID value) {
        return new VenueId(value);
    }

    public static VenueId random() {
        return new VenueId(UUID.randomUUID());
    }
}
