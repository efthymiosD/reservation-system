package com.decoupledx.reservation.webui.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;

/**
 * View model of the admin reservations page. The customer is shown as the
 * stored display name when known (identity module) with the reference prefix
 * as fallback; cancellable means the admin cancel action will apply (ACTIVE
 * reservations).
 */
public record AdminReservationsModel(
        ReservationStatus selectedStatus,
        List<Row> items,
        long total,
        int page,
        int size) {

    public record Row(
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
}
