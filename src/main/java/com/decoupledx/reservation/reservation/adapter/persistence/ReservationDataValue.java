package com.decoupledx.reservation.reservation.adapter.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationDataValue(
        UUID id,
        UUID resourceId,
        String customerId,
        Instant startTime,
        Instant endTime,
        String status,
        BigDecimal priceAmount,
        String priceCurrency,
        Instant createdAt,
        Instant cancelledAt,
        String cancelledBy,
        UUID recurringReservationId) {
}
