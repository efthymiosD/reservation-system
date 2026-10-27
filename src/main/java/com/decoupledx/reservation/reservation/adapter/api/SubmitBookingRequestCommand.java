package com.decoupledx.reservation.reservation.adapter.api;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command of the public booking-request form: the slot values use the same
 * venue-local semantics as the reserve API. The {@code submissionKey} is a
 * per-form-visit dedupe key minted by the form factory (token-hash prefixed,
 * validated module-side).
 */
public record SubmitBookingRequestCommand(
        UUID resourceId,
        LocalDateTime startAt,
        int durationMinutes,
        String customerName,
        String contact,
        String note,
        String submissionKey) {
}
