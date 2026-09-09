package com.decoupledx.reservation.resource.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

/**
 * Module API of the resource module: resource lookups (including locking for
 * concurrent operations) and resource-block use cases. This is the only type
 * other modules may depend on.
 */
public interface ResourceApi {

    ResourceInfo getResource(UUID resourceId);

    ResourceInfo lockResource(UUID resourceId);

    List<ResourceInfo> findActiveResources(UUID venueId);

    ResourceBlockInfo createBlock(CreateBlockCommand command);

    ResourceBlockInfo getBlock(UUID blockId);

    void cancelBlock(UUID blockId, CustomerId actor);

    List<ResourceBlockInfo> findBlocksByResource(UUID resourceId);

    List<ResourceBlockInfo> findAllBlocks();

    List<ResourceBlockInfo> findActiveBlocksOverlapping(Collection<UUID> resourceIds, ReservationPeriod period);
}
