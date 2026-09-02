package com.decoupledx.reservation.resource.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ResourceBlockId(UUID value) {

    public ResourceBlockId {
        Objects.requireNonNull(value, "resource block id must not be null");
    }

    public static ResourceBlockId of(UUID value) {
        return new ResourceBlockId(value);
    }

    public static ResourceBlockId random() {
        return new ResourceBlockId(UUID.randomUUID());
    }
}
