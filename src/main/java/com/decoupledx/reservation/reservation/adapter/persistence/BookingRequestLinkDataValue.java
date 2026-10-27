package com.decoupledx.reservation.reservation.adapter.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence snapshot of a booking-request link (public within the module).
 */
public record BookingRequestLinkDataValue(
        UUID id,
        String label,
        String tokenHash,
        Instant createdAt,
        Instant revokedAt) {
}
