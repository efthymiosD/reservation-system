package com.decoupledx.reservation.resource.adapter.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Names by id (batch lookup without per-id queries). Resources that no
     * longer exist are absent from the map — consumers provide a fallback.
     */
    Map<UUID, String> resourceNames(Collection<UUID> resourceIds);

    ResourceInfo createResource(CreateResourceCommand command);

    void activate(UUID resourceId);

    ResourceInfo deactivate(UUID resourceId);

    void rename(UUID resourceId, String newName);
}