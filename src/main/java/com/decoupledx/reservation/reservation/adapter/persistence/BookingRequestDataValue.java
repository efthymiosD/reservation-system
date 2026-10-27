package com.decoupledx.reservation.reservation.adapter.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistence snapshot of a booking request (public within the module like
 * the reservation snapshot; the api view is the module's outward contract).
 */
public record BookingRequestDataValue(
        UUID id,
        String status,
        String resourceId,
        LocalDateTime startAt,
        int durationMinutes,
        LocalDateTime endAt,
        String customerName,
        String contact,
        String note,
        UUID linkId,
        String submissionKey,
        boolean advisoryFreeAtSubmission,
        Instant createdAt,
        Instant updatedAt,
        Instant decidedAt,
        String decidedBy,
        UUID reservationId) {
}
