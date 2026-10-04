package com.decoupledx.reservation.policy.adapter.web;

public record BookingPolicyGetResponse(
        long minDurationMinutes,
        long maxDurationMinutes,
        long durationStepMinutes,
        long startTimeStepMinutes,
        String maxAdvanceBooking) {
}
