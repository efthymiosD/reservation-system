package com.decoupledx.reservation.webui.reservations;

import java.util.List;

/**
 * View model of the 'My reservations' page: upcoming and past reservations as
 * cards. Cards reference the confirmation page and carry the canCancel UX hint
 * (the cancel use case revalidates the deadline server-side).
 */
record MyReservationsModel(
        List<ReservationCard> upcoming,
        List<ReservationCard> past) {

}
