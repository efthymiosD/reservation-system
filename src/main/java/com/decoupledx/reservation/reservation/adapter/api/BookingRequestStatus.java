package com.decoupledx.reservation.reservation.adapter.api;

/**
 * Lifecycle of a booking request — deliberately separate from
 * {@link ReservationStatus}: a {@code PENDING} request is a statement of
 * intent that reserves nothing and blocks nothing; only an approved request
 * is represented by a real reservation.
 */
public enum BookingRequestStatus {

    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED

}
