package com.decoupledx.reservation.reservation.adapter.api;

import java.time.Instant;
import java.util.UUID;

/**
 * View of a booking-request link (JDK types only). The RAW token is never
 * part of this view — it travels only in a {@link BookingRequestLinkToken}
 * right after issuing/regeneration.
 */
public record BookingRequestLinkInfo(
        UUID id,
        String label,
        Instant createdAt,
        boolean revoked) {
}
