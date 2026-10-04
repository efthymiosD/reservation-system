package com.decoupledx.reservation.resource.domain.port;

import com.decoupledx.reservation.resource.adapter.api.CreateResourceCommand;
import com.decoupledx.reservation.resource.adapter.api.ResourceGroupInfo;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.List;

/**
 * Inbound port for the resource module: resource CRUD and group lookups.
 */
public interface ResourceService {

    ResourceInfo getResource(ResourceId resourceId);

    List<ResourceInfo> findResources(VenueId venueId);

    List<ResourceInfo> findActiveResources(VenueId venueId);

    List<ResourceGroupInfo> findResourceGroups(VenueId venueId);

    ResourceInfo createResource(CreateResourceCommand command);

    ResourceInfo activate(ResourceId resourceId);

    ResourceInfo deactivate(ResourceId resourceId);

    ResourceInfo rename(ResourceId resourceId, String newName);

    ResourceInfo lockResource(ResourceId resourceId);
}
