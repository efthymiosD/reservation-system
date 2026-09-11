package com.decoupledx.reservation.webui.reservations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * View model of the 'My reservations' page: upcoming and past reservations as
 * cards. Cards reference the confirmation page and carry the canCancel UX hint
 * (the cancel use case revalidates the deadline server-side).
 */
public record MyReservationsModel(
        List<ReservationCard> upcoming,
        List<ReservationCard> past) {

    public record ReservationCard(
            UUID reference,
            String fieldName,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            BigDecimal priceAmount,
            String priceCurrency,
            String displayStatus,
            boolean cancellable) {
    }
}
