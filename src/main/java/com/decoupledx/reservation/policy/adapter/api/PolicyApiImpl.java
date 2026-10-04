package com.decoupledx.reservation.policy.adapter.api;

import com.decoupledx.reservation.policy.domain.port.PolicyService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link PolicyApi} on top of the
 * policy inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class PolicyApiImpl implements PolicyApi {

    private final PolicyService policyService;

    @Override
    public BookingPolicy bookingPolicyFor(UUID venueId) {
        return policyService.bookingPolicyFor(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId));
    }

    @Override
    public CancellationPolicy cancellationPolicyFor(UUID venueId) {
        return policyService.cancellationPolicyFor(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId));
    }

    @Override
    public void updateBookingPolicy(UUID venueId, BookingPolicy policy) {
        policyService.updateBookingPolicy(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId), policy);
    }

    @Override
    public void updateCancellationPolicy(UUID venueId, CancellationPolicy policy) {
        policyService.updateCancellationPolicy(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId), policy);
    }
}
