package com.decoupledx.reservation.availability.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AvailableResource(
        UUID resourceId,
        String name,
        String code,
        String type,
        BigDecimal priceAmount,
        String priceCurrency) {

    public AvailableResource {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
