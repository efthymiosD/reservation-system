package com.decoupledx.reservation.administration.domain.service;

import java.util.List;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.resource.domain.model.CreateBlockCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockInfo;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlockResourceService {

    private final ResourceService resourceService;
    private final ReservationQueryService reservationQueries;
    private final TransactionRunner tx;

    public ResourceBlockInfo block(CreateBlockCommand command) {
        return tx.run(() -> {
            resourceService.lockResource(command.resourceId());
            requireNoReservationConflict(command);
            return resourceService.createBlock(command);
        });
    }

    public void cancelBlock(ResourceBlockId blockId, CustomerId actor) {
        tx.run(() -> {
            ResourceBlockInfo block = resourceService.getBlock(blockId);
            resourceService.lockResource(block.resourceId());
            resourceService.cancelBlock(blockId, actor);
        });
    }

    public List<ResourceBlockInfo> findBlocksByResource(ResourceId resourceId) {
        return resourceService.findBlocksByResource(resourceId);
    }

    public List<ResourceBlockInfo> findAllBlocks() {
        return resourceService.findAllBlocks();
    }

    private void requireNoReservationConflict(CreateBlockCommand command) {
        List<ReservationInfo> conflicts = reservationQueries.findActiveOverlappingResource(
                command.resourceId(), command.period());
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_BLOCK_CONFLICT,
                    "Block conflicts with %d active reservation(s); use explicit override to cancel them"
                            .formatted(conflicts.size()));
        }
    }
}
