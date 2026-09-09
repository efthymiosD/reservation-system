package com.decoupledx.reservation.pricing.domain.service;

import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.api.VenueId;
import com.decoupledx.reservation.pricing.api.PricingApi;
import com.decoupledx.reservation.pricing.api.PricingPolicy;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PricingService implements PricingApi {

    private final PricingPolicyRepository pricingPolicies;
    private final TransactionRunner tx;

    @Override
    public PricingPolicy pricingPolicyFor(UUID venueId) {
        return getPricingPolicy(VenueId.of(venueId));
    }

    @Override
    public void updatePricingPolicy(UUID venueId, PricingPolicy policy) {
        updatePricingPolicy(VenueId.of(venueId), policy);
    }

    public PricingPolicy getPricingPolicy(VenueId venueId) {
        return pricingPolicies.findByVenueId(venueId)
                .orElseThrow(() -> new IllegalStateException("No pricing policy configured for venue " + venueId));
    }

    public void updatePricingPolicy(VenueId venueId, PricingPolicy policy) {
        tx.run(() -> pricingPolicies.save(venueId, policy));
    }
}
