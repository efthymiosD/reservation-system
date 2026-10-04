package com.decoupledx.reservation.policy.adapter.persistence;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import java.time.Duration;
import java.time.Period;

/**
 * Persistence view of a booking policy.
 */
public record BookingPolicyDataValue(
        long minDurationMinutes,
        long maxDurationMinutes,
        long durationStepMinutes,
        long startTimeStepMinutes,
        String maxAdvanceBooking) {

    public static BookingPolicyDataValue from(BookingPolicy policy) {
        return new BookingPolicyDataValue(
                policy.minDuration().toMinutes(),
                policy.maxDuration().toMinutes(),
                policy.durationStep().toMinutes(),
                policy.startTimeStep().toMinutes(),
                policy.maxAdvanceBooking().toString());
    }

    public BookingPolicy toBookingPolicy() {
        return new BookingPolicy(
                Duration.ofMinutes(minDurationMinutes),
                Duration.ofMinutes(maxDurationMinutes),
                Duration.ofMinutes(durationStepMinutes),
                Duration.ofMinutes(startTimeStepMinutes),
                Period.parse(maxAdvanceBooking));
    }
}
