package com.decoupledx.reservation.policy.domain.service;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PolicyService {

    private final BookingPolicyRepository bookingPolicies;
    private final CancellationPolicyRepository cancellationPolicies;
    private final TransactionRunner tx;

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
