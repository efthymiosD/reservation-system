package com.decoupledx.reservation.policy.domain.port;

import com.decoupledx.reservation.policy.adapter.persistence.CancellationPolicyDataValue;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.Optional;

public interface CancellationPolicyRepository {

    Optional<CancellationPolicyDataValue> findByVenueId(VenueId venueId);

    void save(VenueId venueId, CancellationPolicyDataValue policy);
}
