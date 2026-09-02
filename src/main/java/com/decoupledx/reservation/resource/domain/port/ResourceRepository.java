package com.decoupledx.reservation.resource.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.Resource;
import com.decoupledx.reservation.venue.domain.model.VenueId;

public interface ResourceRepository {

    Optional<Resource> findById(ResourceId id);

    Optional<Resource> lockById(ResourceId id);

    List<Resource> findByVenueId(VenueId venueId);

    boolean existsByVenueIdAndCode(VenueId venueId, String code);

    Resource save(Resource resource);
}
