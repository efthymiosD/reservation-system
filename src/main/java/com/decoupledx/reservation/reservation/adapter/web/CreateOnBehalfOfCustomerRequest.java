package com.decoupledx.reservation.reservation.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Administrative booking-on-behalf request (staff placement). The
 * same-customer overlap invariant is relaxed for such bookings; a single
 * pitch still cannot host two bookings in the same window.
 */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = false)
public record CreateOnBehalfOfCustomerRequest(
        @NotNull UUID resourceId,
        @NotNull String customerId,
        @NotNull LocalDateTime startTime,
        @Positive int durationMinutes) {
}
