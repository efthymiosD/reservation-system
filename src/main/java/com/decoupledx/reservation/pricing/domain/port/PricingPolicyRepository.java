package com.decoupledx.reservation.pricing.domain.port;

import java.util.Optional;

import com.decoupledx.reservation.pricing.api.PricingPolicy;
import com.decoupledx.reservation.venue.api.VenueId;

public interface PricingPolicyRepository {

    Optional<PricingPolicy> findByVenueId(VenueId venueId);

    void save(VenueId venueId, PricingPolicy policy);
}
