package com.decoupledx.reservation.pricing.domain.service;

import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.pricing.domain.model.PricingPolicy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PricingService {

    private final PricingPolicyRepository pricingPolicies;
    private final TransactionRunner tx;

    public PricingPolicy getPricingPolicy(VenueId venueId) {
        return pricingPolicies.findByVenueId(venueId)
                .orElseThrow(() -> new IllegalStateException("No pricing policy configured for venue " + venueId));
    }

    public void updatePricingPolicy(VenueId venueId, PricingPolicy policy) {
        tx.run(() -> pricingPolicies.save(venueId, policy));
    }
}
