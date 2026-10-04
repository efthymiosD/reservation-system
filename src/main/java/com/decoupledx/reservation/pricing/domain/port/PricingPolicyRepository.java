package com.decoupledx.reservation.pricing.domain.port;

import com.decoupledx.reservation.pricing.adapter.persistence.PricingPolicyDataValue;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.Optional;

public interface PricingPolicyRepository {

    Optional<PricingPolicyDataValue> findByVenueId(VenueId venueId);

    void save(VenueId venueId, PricingPolicyDataValue policy);
}
