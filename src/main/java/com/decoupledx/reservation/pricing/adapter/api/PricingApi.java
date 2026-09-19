package com.decoupledx.reservation.pricing.adapter.api;

import java.util.UUID;

/**
 * Module API of the pricing module.
 */
public interface PricingApi {

    PricingPolicy pricingPolicyFor(UUID venueId);

    void updatePricingPolicy(UUID venueId, PricingPolicy policy);
}
