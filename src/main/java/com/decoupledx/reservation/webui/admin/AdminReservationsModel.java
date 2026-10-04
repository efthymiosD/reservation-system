package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import java.util.List;

/**
 * View model of the admin reservations page. The customer is shown as the
 * stored display name when known (identity module) with the reference prefix
 * as fallback; cancellable means the admin cancel action will apply (ACTIVE
 * reservations).
 */
record AdminReservationsModel(
        ReservationStatus selectedStatus,
        List<Row> items,
        long total,
        int page,
        int size) {

}
