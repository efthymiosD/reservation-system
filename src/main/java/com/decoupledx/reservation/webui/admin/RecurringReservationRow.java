package com.decoupledx.reservation.webui.admin;

import java.util.UUID;

/**
 * Times are venue-local; the venue's timezone is not re-rendered per row.
 */
record RecurringReservationRow(UUID reference, String fieldName, String customerName,
                               String weekday, String startTime, String endTime,
                               String windowLabel, String status, boolean active) {
}
