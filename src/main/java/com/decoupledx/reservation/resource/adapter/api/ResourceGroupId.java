package com.decoupledx.reservation.resource.adapter.api;

import java.util.Objects;
import java.util.UUID;

public record ResourceGroupId(UUID value) {

    public ResourceGroupId {
        Objects.requireNonNull(value, "resource group id must not be null");
    }

    public static ResourceGroupId of(UUID value) {
        return new ResourceGroupId(value);
    }

    public static ResourceGroupId random() {
        return new ResourceGroupId(UUID.randomUUID());
    }
}
