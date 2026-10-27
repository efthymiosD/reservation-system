package com.decoupledx.reservation.webui.bookingrequest;

import java.util.UUID;

/**
 * One selectable field/resource of the public booking-request form.
 */
record ResourceOption(
        UUID id,
        String name) {
}
