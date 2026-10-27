package com.decoupledx.reservation.webui.bookingrequest;

import java.time.LocalTime;
import java.util.List;

/**
 * Advisory option set shown on the public booking-request form.
 */
record BookingRequestFormOptions(
        String minDate,
        String maxDate,
        List<Integer> durationOptions,
        List<LocalTime> startOptions,
        List<ResourceOption> resources) {
}
