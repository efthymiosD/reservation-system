package com.decoupledx.reservation.webui.bookingrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The public booking-request form payload. Server-side validation is the
 * authority; the message keys resolve from the public bundles
 * ({@code bookingrequest.*}), so errors are localized. The
 * {@code submissionKey} is minted by the form factory per page render and
 * makes refresh/back-button resubmission idempotent.
 */
record BookingRequestSubmission(
        @NotNull(message = "{bookingrequest.error.fieldRequired}") UUID resourceId,
        @NotNull(message = "{bookingrequest.error.fieldRequired}") LocalDate date,
        @NotNull(message = "{bookingrequest.error.fieldRequired}") LocalTime start,
        @NotNull(message = "{bookingrequest.error.fieldRequired}") Integer durationMinutes,
        @NotBlank(message = "{bookingrequest.error.nameRequired}") @Size(max = 120, message = "{bookingrequest.error.tooLong}") String customerName,
        @NotBlank(message = "{bookingrequest.error.contactRequired}") @Size(max = 200, message = "{bookingrequest.error.tooLong}") String contact,
        @Size(max = 500, message = "{bookingrequest.error.tooLong}") String note,
        String submissionKey) {
}
