package com.decoupledx.reservation.webui.admin;

import java.time.LocalTime;
import java.util.List;

/**
 * View model of the admin recurring-reservations page: the recurring-reservation
 * rows plus the active resources and provisioned customers usable as picker
 * options, the predefined booking windows (1/3/6 months) and the same start-time
 * and duration choices as the customer reserve page.
 */
record AdminRecurringReservationsModel(
        List<ResourceOption> resources,
        List<CustomerOption> customers,
        List<WindowOption> windows,
        List<LocalTime> startTimeOptions,
        List<Integer> durationOptions,
        List<RecurringReservationRow> reservations) {

}
