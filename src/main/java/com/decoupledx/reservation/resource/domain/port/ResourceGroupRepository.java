package com.decoupledx.reservation.resource.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceGroup;
import com.decoupledx.reservation.venue.domain.model.VenueId;

public interface ResourceGroupRepository {

    Optional<ResourceGroup> findById(ResourceGroupId id);

    List<ResourceGroup> findByVenueId(VenueId venueId);

    ResourceGroup save(ResourceGroup group);
}
