package com.decoupledx.reservation.webui.confirmation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * View model of the reservation confirmation page. Shows the stored (snapshot)
 * price; the reference is the reservation id.
 */
public record ConfirmationModel(
        UUID reference,
        String fieldName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        long durationMinutes,
        BigDecimal priceAmount,
        String priceCurrency) {
}
