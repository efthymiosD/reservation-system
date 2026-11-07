package com.decoupledx.reservation.resource.domain.port;

import com.decoupledx.reservation.resource.adapter.api.CreateResourceCommand;
import com.decoupledx.reservation.resource.adapter.api.ResourceGroupInfo;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound port for the resource module: resource CRUD and group lookups.
 */
public interface ResourceService {

    ResourceInfo getResource(ResourceId resourceId);

    List<ResourceInfo> findResources(VenueId venueId);

    /**
     * Names by id (batch lookup without per-id queries); rows that no longer
     * exist are absent from the map.
     */
    Map<UUID, String> resourceNames(Collection<ResourceId> resourceIds);

    List<ResourceInfo> findActiveResources(VenueId venueId);

    List<ResourceGroupInfo> findResourceGroups(VenueId venueId);

    ResourceInfo createResource(CreateResourceCommand command);

    ResourceInfo activate(ResourceId resourceId);

    ResourceInfo deactivate(ResourceId resourceId);

    ResourceInfo rename(ResourceId resourceId, String newName);

    ResourceInfo lockResource(ResourceId resourceId);
}
