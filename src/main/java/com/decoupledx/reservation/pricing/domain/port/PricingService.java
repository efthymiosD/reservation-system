package com.decoupledx.reservation.pricing.domain.port;

import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.venue.adapter.api.VenueId;

/**
 * Inbound port for the pricing module: pricing policy lookups and updates.
 */
public interface PricingService {

    PricingPolicy pricingPolicyFor(VenueId venueId);

    void updatePricingPolicy(VenueId venueId, PricingPolicy policy);
}
