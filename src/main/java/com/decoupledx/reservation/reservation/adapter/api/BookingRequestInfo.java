package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * View of a booking request (api/JDK/resource-api types only, mirroring
 * {@link ReservationInfo}'s conventions): slot values as venue-local
 * LocalDateTime (the create-API surface), bookkeeping timestamps as Instant.
 * The {@code reference} is the customer-facing short form; the internal id
 * is not part of any public URL.
 */
public record BookingRequestInfo(
        UUID id,
        String reference,
        BookingRequestStatus status,
        ResourceId resourceId,
        LocalDateTime startAt,
        int durationMinutes,
        LocalDateTime endAt,
        String customerName,
        String contact,
        String note,
        boolean advisoryFreeAtSubmission,
        Instant createdAt,
        Instant decidedAt,
        String decidedBy,
        UUID reservationId) {
}
