package com.decoupledx.reservation.administration.domain;

import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.administration.api.AdministrationApi;
import com.decoupledx.reservation.administration.api.OverrideResult;
import com.decoupledx.reservation.administration.domain.service.BlockResourceService;
import com.decoupledx.reservation.administration.domain.service.OverrideResourceBlockService;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.resource.api.CreateBlockCommand;
import com.decoupledx.reservation.resource.api.ResourceBlockId;
import com.decoupledx.reservation.resource.api.ResourceBlockInfo;
import com.decoupledx.reservation.resource.api.ResourceId;

import lombok.RequiredArgsConstructor;

/**
 * Module-internal facade implementing {@link AdministrationApi} on top of the
 * block and override use-case services. The only bean other modules use for
 * this module.
 */
@RequiredArgsConstructor
public class BlockManagementFacade implements AdministrationApi {

    private final BlockResourceService blockResourceService;
    private final OverrideResourceBlockService overrideResourceBlockService;

    @Override
    public ResourceBlockInfo block(CreateBlockCommand command) {
        return blockResourceService.block(command);
    }

    @Override
    public OverrideResult override(CreateBlockCommand command, CustomerId actor) {
        OverrideResourceBlockService.OverrideResult result =
                overrideResourceBlockService.override(command, actor);
        return new OverrideResult(result.block(), result.cancelledReservations());
    }

    @Override
    public void cancelBlock(UUID blockId, CustomerId actor) {
        blockResourceService.cancelBlock(ResourceBlockId.of(blockId), actor);
    }

    @Override
    public List<ResourceBlockInfo> findBlocksByResource(UUID resourceId) {
        return blockResourceService.findBlocksByResource(ResourceId.of(resourceId));
    }

    @Override
    public List<ResourceBlockInfo> findAllBlocks() {
        return blockResourceService.findAllBlocks();
    }
}
