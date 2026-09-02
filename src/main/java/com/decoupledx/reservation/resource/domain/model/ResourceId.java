package com.decoupledx.reservation.resource.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ResourceId(UUID value) {

    public ResourceId {
        Objects.requireNonNull(value, "resource id must not be null");
    }

    public static ResourceId of(UUID value) {
        return new ResourceId(value);
    }

    public static ResourceId random() {
        return new ResourceId(UUID.randomUUID());
    }
}
