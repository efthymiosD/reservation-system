package com.decoupledx.reservation.resource.domain.port;

import com.decoupledx.reservation.resource.adapter.api.ResourceGroupId;
import com.decoupledx.reservation.resource.adapter.persistence.ResourceGroupDataValue;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.List;
import java.util.Optional;

public interface ResourceGroupRepository {

    Optional<ResourceGroupDataValue> findById(ResourceGroupId id);

    List<ResourceGroupDataValue> findByVenueId(VenueId venueId);
}
