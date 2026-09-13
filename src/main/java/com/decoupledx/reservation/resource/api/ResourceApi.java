package com.decoupledx.reservation.resource.api;

import java.util.List;
import java.util.UUID;

/**
 * Module API of the resource module: resource lookups (including locking for
 * concurrent operations). This is the only type other modules may depend on.
 */
public interface ResourceApi {

    ResourceInfo getResource(UUID resourceId);

    ResourceInfo lockResource(UUID resourceId);

    List<ResourceInfo> findActiveResources(UUID venueId);

    List<ResourceInfo> findResources(UUID venueId);

    ResourceInfo createResource(CreateResourceCommand command);

    ResourceInfo activate(UUID resourceId);

    ResourceInfo deactivate(UUID resourceId);

    ResourceInfo rename(UUID resourceId, String newName);
}