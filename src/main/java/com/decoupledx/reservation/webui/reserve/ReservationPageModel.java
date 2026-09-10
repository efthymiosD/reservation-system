package com.decoupledx.reservation.webui.reserve;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * View model of the reservation page. Everything is server-shaped from backend
 * configuration (durations, time grid, advance window, availability, price) so
 * client JavaScript implements no business rules.
 */
public record ReservationPageModel(
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
        boolean anyAvailable) {

    public record MoneyView(BigDecimal amount, String currency) {
    }

    /** An own active reservation overlapping the selected slot, for the UI hint. */
    public record HeldReservation(UUID reservationId, String fieldName, LocalTime start, LocalTime end) {
    }

    public record MapField(
            UUID resourceId,
            String label,
            int x,
            int y,
            int width,
            int height,
            String status,
            String statusClass,
            String ariaLabel) {
    }
}
