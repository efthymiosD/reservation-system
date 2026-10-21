package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * View model of the admin reservations page: the full filter echo (status,
 * customer name, date window), the picker options and range-cap signal, plus
 * resolved rows. The customer is shown as the stored display name when known
 * (identity module) with the reference prefix as fallback; cancellable means
 * the admin cancel action will apply (ACTIVE reservations).
 */
record AdminReservationsModel(
        ReservationStatus selectedStatus,
        String selectedCustomer,
        LocalDate selectedFrom,
        LocalDate selectedTo,
        List<String> customerNameOptions,
        boolean rangeClamped,
        List<Row> items,
        long total,
        int page,
        int size) {

}
