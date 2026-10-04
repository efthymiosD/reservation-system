package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * View model of the reservation page. Everything is server-shaped from backend
 * configuration (durations, time grid, advance window, availability, price) so
 * client JavaScript implements no business rules.
 */
record ReservationPageModel(
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        LocalDate minDate,
        LocalDate maxDate,
        List<Integer> durationOptions,
        List<LocalTime> timeOptions,
        MoneyView price,
        List<MapField> fields,
        HeldReservation held,
        boolean anyAvailable,
        int canvasWidth,
        int canvasHeight) {

}
