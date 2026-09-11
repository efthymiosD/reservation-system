package com.decoupledx.reservation.administration.api;

import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.resource.api.CreateBlockCommand;
import com.decoupledx.reservation.resource.api.ResourceBlockInfo;

/**
 * Module API of the administration module: resource-block management use cases
 * (create with conflict checks, admin cancel, and the atomic override that
 * cancels conflicting reservations before the block is created). The only type
 * other modules may depend on.
 */
public interface AdministrationApi {

    ResourceBlockInfo block(CreateBlockCommand command);

    OverrideResult override(CreateBlockCommand command, CustomerId actor);

    void cancelBlock(UUID blockId, CustomerId actor);

    List<ResourceBlockInfo> findBlocksByResource(UUID resourceId);

    List<ResourceBlockInfo> findAllBlocks();
}