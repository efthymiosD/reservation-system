package com.decoupledx.reservation.availability.adapter.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * View-oriented availability of a single resource for one requested slot.
 * The status is derived from backend state (overlapping active reservations);
 * the price is the backend-computed price for the slot.
 */
public record ResourceAvailability(
        UUID resourceId,
        String name,
        String code,
        String type,
        ResourceAvailabilityStatus status,
        BigDecimal priceAmount,
        String priceCurrency) {
}
