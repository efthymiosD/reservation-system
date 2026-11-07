package com.decoupledx.reservation.policy.adapter.web;


record BookingPolicyPublicResponse(
        long minDurationMinutes,
        long maxDurationMinutes,
        long durationStepMinutes,
        long startTimeStepMinutes,
        String maxAdvanceBooking) {
}
