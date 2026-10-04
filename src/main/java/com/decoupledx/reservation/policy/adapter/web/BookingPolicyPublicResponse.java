package com.decoupledx.reservation.policy.adapter.web;


public record BookingPolicyPublicResponse(
        long minDurationMinutes,
        long maxDurationMinutes,
        long durationStepMinutes,
        long startTimeStepMinutes,
        String maxAdvanceBooking) {
}
