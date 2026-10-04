package com.decoupledx.reservation.policy.domain;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.policy.adapter.persistence.BookingPolicyDataValue;
import com.decoupledx.reservation.policy.adapter.persistence.CancellationPolicyDataValue;
import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.PolicyService;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PolicyServiceImpl implements PolicyService {

    private final BookingPolicyRepository bookingPolicies;
    private final CancellationPolicyRepository cancellationPolicies;
    private final TransactionRunner tx;

    @Override
    public BookingPolicy bookingPolicyFor(VenueId venueId) {
        return bookingPolicies.findByVenueId(venueId)
                .map(BookingPolicyDataValue::toBookingPolicy)
                .orElseThrow(() -> new IllegalStateException("No booking policy configured for venue " + venueId));
    }

    @Override
    public CancellationPolicy cancellationPolicyFor(VenueId venueId) {
        return cancellationPolicies.findByVenueId(venueId)
                .map(CancellationPolicyDataValue::toCancellationPolicy)
                .orElseThrow(() -> new IllegalStateException("No cancellation policy configured for venue " + venueId));
    }

    @Override
    public void updateBookingPolicy(VenueId venueId, BookingPolicy policy) {
        tx.run(() -> bookingPolicies.save(venueId, BookingPolicyDataValue.from(policy)));
    }

    @Override
    public void updateCancellationPolicy(VenueId venueId, CancellationPolicy policy) {
        tx.run(() -> cancellationPolicies.save(venueId, CancellationPolicyDataValue.from(policy)));
    }
}
