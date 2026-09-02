package com.decoupledx.reservation.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "customer id must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("customer id must not be blank");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    public static CustomerId random() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
