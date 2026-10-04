package com.decoupledx.reservation.webui.reservations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record ReservationCard(
        UUID reference,
        String fieldName,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        BigDecimal priceAmount,
        String priceCurrency,
        String displayStatus,
        boolean cancellable) {
}
