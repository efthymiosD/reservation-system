package com.decoupledx.reservation.policy.domain.port;

import java.util.Optional;

import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.venue.adapter.api.VenueId;

public interface CancellationPolicyRepository {

    Optional<CancellationPolicy> findByVenueId(VenueId venueId);

    void save(VenueId venueId, CancellationPolicy policy);
}
