package com.decoupledx.reservation.reservation.adapter.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminReservationResponse(
        UUID id,
        UUID resourceId,
        String customerId,
        Instant start,
        Instant end,
        String status,
        BigDecimal priceAmount,
        String priceCurrency,
        Instant createdAt,
        Instant cancelledAt,
        UUID recurringReservationId) {
}
