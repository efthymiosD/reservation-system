package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.shared.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A reservation was created. Published after the aggregate is saved, inside
 * the creating transaction; consumers run after commit.
 */
public record ReservationCreated(
        UUID reservationId,
        UUID resourceId,
        String customerId,
        Instant start,
        Instant end,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean adminOverride,
        UUID recurringReservationId) implements DomainEvent {
}
