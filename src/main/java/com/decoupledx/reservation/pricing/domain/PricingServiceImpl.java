package com.decoupledx.reservation.pricing.domain;

import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.pricing.adapter.persistence.PricingPolicyDataValue;
import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.pricing.domain.port.PricingService;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PricingServiceImpl implements PricingService {

    private final PricingPolicyRepository pricingPolicies;
    private final TransactionRunner tx;

    @Override
    public PricingPolicy pricingPolicyFor(VenueId venueId) {
        return pricingPolicies.findByVenueId(venueId)
                .map(data -> new PricingPolicy(data.hourlyPrice()))
                .orElseThrow(() -> new IllegalStateException("No pricing policy configured for venue " + venueId));
    }

    @Override
    public void updatePricingPolicy(VenueId venueId, PricingPolicy policy) {
        tx.run(() -> {
            PricingPolicyDataValue data = new PricingPolicyDataValue(venueId.value(), policy.hourlyPrice());
            pricingPolicies.save(venueId, data);
        });
    }
}
