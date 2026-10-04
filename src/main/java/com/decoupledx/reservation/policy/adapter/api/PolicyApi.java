package com.decoupledx.reservation.policy.adapter.api;

import java.util.UUID;

/**
 * Module API of the policy module: read access to the booking and cancellation
 * policies (their views carry the validation rules) plus admin updates.
 */
public interface PolicyApi {

    BookingPolicy bookingPolicyFor(UUID venueId);

    CancellationPolicy cancellationPolicyFor(UUID venueId);

    void updateBookingPolicy(UUID venueId, BookingPolicy policy);

    void updateCancellationPolicy(UUID venueId, CancellationPolicy policy);
}
