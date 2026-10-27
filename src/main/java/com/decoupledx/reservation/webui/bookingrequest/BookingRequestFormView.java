package com.decoupledx.reservation.webui.bookingrequest;

/**
 * The form page view model: advisory options, the resolved (or still-raw,
 * per-route) token and the per-visit submission key for the hidden field.
 */
record BookingRequestFormView(
        BookingRequestFormOptions options,
        String token,
        String submissionKey) {
}
