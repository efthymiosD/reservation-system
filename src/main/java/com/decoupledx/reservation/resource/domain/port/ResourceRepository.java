package com.decoupledx.reservation.resource.domain.port;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.persistence.ResourceDataValue;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResourceRepository {

    Optional<ResourceDataValue> findById(ResourceId id);

    Optional<ResourceDataValue> lockById(ResourceId id);

    List<ResourceDataValue> findByVenueId(VenueId venueId);

    /**
     * Names by id (batch lookup without per-id queries); rows that no longer
     * exist are absent from the map.
     */
    Map<ResourceId, String> names(Collection<ResourceId> resourceIds);

    boolean existsByVenueIdAndCode(VenueId venueId, String code);

    ResourceDataValue save(ResourceDataValue resource);
}
