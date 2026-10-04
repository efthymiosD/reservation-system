package com.decoupledx.reservation.webui.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

record Row(
        UUID reference,
        String customerName,
        String customerId,
        String fieldName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal priceAmount,
        String priceCurrency,
        String displayStatus,
        boolean cancellable) {
}
