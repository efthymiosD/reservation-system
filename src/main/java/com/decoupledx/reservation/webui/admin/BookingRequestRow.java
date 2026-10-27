package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One display row of the coach's booking-request queue: the api info plus the
 * venue-local slot split and the request age.
 */
record BookingRequestRow(
        BookingRequestInfo info,
        LocalDate slotDate,
        LocalTime slotStart,
        LocalTime slotEnd,
        long ageDays) {
}
