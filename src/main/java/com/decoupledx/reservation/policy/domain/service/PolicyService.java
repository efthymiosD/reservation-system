package com.decoupledx.reservation.policy.domain.service;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.api.VenueId;
import com.decoupledx.reservation.policy.api.BookingPolicy;
import com.decoupledx.reservation.policy.api.PolicyApi;
import java.util.UUID;
import com.decoupledx.reservation.policy.api.CancellationPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PolicyService implements PolicyApi {

    private final BookingPolicyRepository bookingPolicies;
    private final CancellationPolicyRepository cancellationPolicies;
    private final TransactionRunner tx;

    @Override
    public BookingPolicy bookingPolicyFor(UUID venueId) {
        return getBookingPolicy(VenueId.of(venueId));
    }

    @Override
    public CancellationPolicy cancellationPolicyFor(UUID venueId) {
        return getCancellationPolicy(VenueId.of(venueId));
    }

    @Override
    public void updateBookingPolicy(UUID venueId, BookingPolicy policy) {
        updateBookingPolicy(VenueId.of(venueId), policy);
    }

    @Override
    public void updateCancellationPolicy(UUID venueId, CancellationPolicy policy) {
        updateCancellationPolicy(VenueId.of(venueId), policy);
    }

    public BookingPolicy getBookingPolicy(VenueId venueId) {
        return bookingPolicies.findByVenueId(venueId)
                .orElseThrow(() -> new IllegalStateException("No booking policy configured for venue " + venueId));
    }

    public void updateBookingPolicy(VenueId venueId, BookingPolicy policy) {
        tx.run(() -> bookingPolicies.save(venueId, policy));
    }

    public CancellationPolicy getCancellationPolicy(VenueId venueId) {
        return cancellationPolicies.findByVenueId(venueId)
                .orElseThrow(() -> new IllegalStateException("No cancellation policy configured for venue " + venueId));
    }

    public void updateCancellationPolicy(VenueId venueId, CancellationPolicy policy) {
        tx.run(() -> cancellationPolicies.save(venueId, policy));
    }
}
