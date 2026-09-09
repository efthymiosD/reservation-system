package com.decoupledx.reservation.administration.domain.service;

import java.util.List;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.CreateBlockCommand;
import com.decoupledx.reservation.resource.api.ResourceBlockId;
import com.decoupledx.reservation.resource.api.ResourceId;
import com.decoupledx.reservation.resource.api.ResourceBlockInfo;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlockResourceService {

    private final ResourceApi resourceService;
    private final ReservationApi reservationQueries;
    private final TransactionRunner tx;

    public ResourceBlockInfo block(CreateBlockCommand command) {
        return tx.run(() -> {
            resourceService.lockResource(command.resourceId().value());
            requireNoReservationConflict(command);
            return resourceService.createBlock(command);
        });
    }

    public void cancelBlock(ResourceBlockId blockId, CustomerId actor) {
        tx.run(() -> {
            ResourceBlockInfo block = resourceService.getBlock(blockId.value());
            resourceService.lockResource(block.resourceId().value());
            resourceService.cancelBlock(blockId.value(), actor);
        });
    }

    public List<ResourceBlockInfo> findBlocksByResource(ResourceId resourceId) {
        return resourceService.findBlocksByResource(resourceId.value());
    }

    public List<ResourceBlockInfo> findAllBlocks() {
        return resourceService.findAllBlocks();
    }

    private void requireNoReservationConflict(CreateBlockCommand command) {
        List<ReservationInfo> conflicts = reservationQueries.findActiveOverlappingResource(
                command.resourceId().value(), command.period());
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_BLOCK_CONFLICT,
                    "Block conflicts with %d active reservation(s); use explicit override to cancel them"
                            .formatted(conflicts.size()));
        }
    }
}
