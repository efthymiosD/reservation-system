package com.decoupledx.reservation.pricing.adapter.api;

import com.decoupledx.reservation.pricing.domain.port.PricingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link PricingApi} on top of the
 * pricing inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class PricingApiImpl implements PricingApi {

    private final PricingService pricingService;

    @Override
    public PricingPolicy pricingPolicyFor(UUID venueId) {
        return pricingService.pricingPolicyFor(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId));
    }

    @Override
    public void updatePricingPolicy(UUID venueId, PricingPolicy policy) {
        pricingService.updatePricingPolicy(com.decoupledx.reservation.venue.adapter.api.VenueId.of(venueId), policy);
    }
}
