package com.decoupledx.reservation.webui.reserve;

import java.time.LocalTime;
import java.util.UUID;

/**
 * An own active reservation overlapping the selected slot, for the UI hint.
 */
record HeldReservation(UUID reservationId, String fieldName, LocalTime start, LocalTime end) {
}
