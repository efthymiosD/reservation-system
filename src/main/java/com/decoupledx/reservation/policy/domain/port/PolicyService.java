package com.decoupledx.reservation.policy.domain.port;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.venue.adapter.api.VenueId;

/**
 * Inbound port for the policy module: booking and cancellation policy access.
 */
public interface PolicyService {

    BookingPolicy bookingPolicyFor(VenueId venueId);

    CancellationPolicy cancellationPolicyFor(VenueId venueId);

    void updateBookingPolicy(VenueId venueId, BookingPolicy policy);

    void updateCancellationPolicy(VenueId venueId, CancellationPolicy policy);
}
