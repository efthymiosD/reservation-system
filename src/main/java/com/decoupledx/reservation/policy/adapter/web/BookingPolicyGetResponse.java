package com.decoupledx.reservation.policy.adapter.web;

record BookingPolicyGetResponse(
        long minDurationMinutes,
        long maxDurationMinutes,
        long durationStepMinutes,
        long startTimeStepMinutes,
        String maxAdvanceBooking) {
}
