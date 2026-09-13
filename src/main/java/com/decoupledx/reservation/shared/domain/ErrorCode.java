package com.decoupledx.reservation.shared.domain;

public enum ErrorCode {

    RESOURCE_NOT_FOUND(404, "Resource not found"),
    RESOURCE_INACTIVE(409, "Resource is inactive"),
    RESOURCE_NO_LONGER_AVAILABLE(409, "Resource is no longer available for the requested period"),
    CUSTOMER_HAS_OVERLAPPING_RESERVATION(409, "Customer already has an overlapping active reservation"),
    RESERVATION_NOT_FOUND(404, "Reservation not found"),
    RESERVATION_ALREADY_CANCELLED(409, "Reservation is already cancelled"),
    RESERVATION_IN_PAST(409, "Reservation is in the past and can no longer be cancelled"),
    CANCELLATION_DEADLINE_PASSED(409, "Cancellation deadline has passed"),
    INVALID_RESERVATION_DURATION(422, "Invalid reservation duration"),
    INVALID_START_TIME(422, "Start time does not match the allowed increment"),
    START_TIME_IN_PAST(422, "Start time is in the past"),
    OUTSIDE_OPENING_HOURS(422, "Requested period is outside opening hours"),
    ADVANCE_BOOKING_LIMIT_EXCEEDED(422, "Reservation is too far in the future"),
    INVALID_RESERVATION_PERIOD(422, "Invalid reservation period"),
    RECURRING_RESERVATION_NOT_FOUND(404, "Recurring reservation not found"),
    RECURRING_RESERVATION_ALREADY_CANCELLED(409, "Recurring reservation is already cancelled"),
    INVALID_RECURRING_RESERVATION_PERIOD(422, "Recurring reservation end time must be after start time"),
    INVALID_RECURRING_RESERVATION_WINDOW(422, "Recurring reservation window must be one of the predefined booking windows"),
    VENUE_NOT_FOUND(404, "Venue not found"),
    RESOURCE_GROUP_NOT_FOUND(404, "Resource group not found"),
    RESOURCE_CODE_EXISTS(409, "A resource with this code already exists"),
    INVALID_OPENING_HOURS(422, "Closing time must be after opening time"),
    INVALID_BOOKING_POLICY(422, "Invalid booking policy configuration"),
    INVALID_CANCELLATION_POLICY(422, "Invalid cancellation policy configuration"),
    INVALID_PRICING_POLICY(422, "Invalid pricing policy configuration");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
