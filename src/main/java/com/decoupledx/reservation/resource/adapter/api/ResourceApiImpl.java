package com.decoupledx.reservation.resource.adapter.api;

import com.decoupledx.reservation.resource.domain.port.ResourceService;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link ResourceApi} on top of the
 * resource inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class ResourceApiImpl implements ResourceApi {

    private final ResourceService resourceService;

    @Override
    public ResourceInfo getResource(UUID resourceId) {
        return resourceService.getResource(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo lockResource(UUID resourceId) {
        return resourceService.lockResource(ResourceId.of(resourceId));
    }

    @Override
    public List<ResourceInfo> findActiveResources(UUID venueId) {
        return resourceService.findActiveResources(VenueId.of(venueId));
    }

    @Override
    public List<ResourceInfo> findResources(UUID venueId) {
        return resourceService.findResources(VenueId.of(venueId));
    }

    @Override
    public ResourceInfo createResource(CreateResourceCommand command) {
        return resourceService.createResource(command);
    }

    @Override
    public void activate(UUID resourceId) {
        resourceService.activate(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo deactivate(UUID resourceId) {
        return resourceService.deactivate(ResourceId.of(resourceId));
    }

    @Override
    public void rename(UUID resourceId, String newName) {
        resourceService.rename(ResourceId.of(resourceId), newName);
    }
}
