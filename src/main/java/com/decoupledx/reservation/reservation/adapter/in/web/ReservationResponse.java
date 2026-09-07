package com.decoupledx.reservation.reservation.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record ReservationResponse(
        UUID id,
        UUID resourceId,
        Instant start,
        Instant end,
        String status,
        BigDecimal priceAmount,
        String priceCurrency,
        Instant createdAt,
        Instant cancelledAt,
        boolean canCancel) {
}
