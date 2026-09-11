package com.decoupledx.reservation.resource.domain.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import com.decoupledx.reservation.resource.api.CreateBlockCommand;
import java.util.UUID;

import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.resource.api.CreateResourceCommand;
import com.decoupledx.reservation.resource.api.ResourceBlockId;
import com.decoupledx.reservation.resource.api.ResourceBlockInfo;
import com.decoupledx.reservation.resource.domain.model.ResourceGroupInfo;
import com.decoupledx.reservation.resource.api.ResourceId;
import com.decoupledx.reservation.resource.api.ResourceInfo;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.resource.domain.model.Resource;
import com.decoupledx.reservation.resource.domain.model.ResourceBlock;
import com.decoupledx.reservation.resource.domain.model.ResourceGroup;
import com.decoupledx.reservation.resource.domain.port.ResourceBlockRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.api.VenueId;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceService implements ResourceApi {

    private final ResourceRepository resources;
    private final ResourceGroupRepository groups;
    private final ResourceBlockRepository blocks;
    private final TransactionRunner tx;

    @Override
    public ResourceInfo getResource(UUID resourceId) {
        return getResource(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo lockResource(UUID resourceId) {
        return lockResource(ResourceId.of(resourceId));
    }

    @Override
    public List<ResourceInfo> findResources(UUID venueId) {
        return findResources(VenueId.of(venueId));
    }

    @Override
    public ResourceInfo activate(UUID resourceId) {
        return activate(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo deactivate(UUID resourceId) {
        return deactivate(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo rename(UUID resourceId, String newName) {
        return rename(ResourceId.of(resourceId), newName);
    }

    @Override
    public List<ResourceInfo> findActiveResources(UUID venueId) {
        return findActiveResources(VenueId.of(venueId));
    }

    @Override
    public ResourceBlockInfo getBlock(UUID blockId) {
        return getBlock(ResourceBlockId.of(blockId));
    }

    @Override
    public void cancelBlock(UUID blockId, CustomerId actor) {
        cancelBlock(ResourceBlockId.of(blockId), actor);
    }

    @Override
    public List<ResourceBlockInfo> findBlocksByResource(UUID resourceId) {
        return findBlocksByResource(ResourceId.of(resourceId));
    }

    public ResourceInfo getResource(ResourceId resourceId) {
        return toInfo(loadResource(resourceId));
    }

    public List<ResourceInfo> findResources(VenueId venueId) {
        return resources.findByVenueId(venueId).stream().map(this::toInfo).toList();
    }

    public List<ResourceInfo> findActiveResources(VenueId venueId) {
        return resources.findByVenueId(venueId).stream()
                .filter(Resource::isActive)
                .map(this::toInfo)
                .toList();
    }

    public List<ResourceGroupInfo> findResourceGroups(VenueId venueId) {
        return groups.findByVenueId(venueId).stream().map(this::toGroupInfo).toList();
    }

    @Override
    public ResourceInfo createResource(CreateResourceCommand command) {
        return tx.run(() -> {
            groups.findById(command.groupId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_GROUP_NOT_FOUND));
            if (resources.existsByVenueIdAndCode(command.venueId(), command.code())) {
                throw new BusinessException(ErrorCode.RESOURCE_CODE_EXISTS);
            }
            Resource resource = Resource.create(
                    command.groupId(), command.venueId(), command.name(), command.code(),
                    command.type(), Instant.now());
            return toInfo(resources.save(resource));
        });
    }

    public ResourceInfo activate(ResourceId resourceId) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.activate();
            return toInfo(resources.save(resource));
        });
    }

    public ResourceInfo deactivate(ResourceId resourceId) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.deactivate();
            return toInfo(resources.save(resource));
        });
    }

    public ResourceInfo rename(ResourceId resourceId, String newName) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.rename(newName);
            return toInfo(resources.save(resource));
        });
    }

    public ResourceInfo lockResource(ResourceId resourceId) {
        return tx.run(() -> toInfo(resources.lockById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }

    public ResourceBlockInfo createBlock(CreateBlockCommand command) {
        return tx.run(() -> {
            ResourceBlock block = ResourceBlock.create(
                    command.resourceId(), command.period(), command.reason(), Instant.now());
            return toBlockInfo(blocks.save(block));
        });
    }

    public ResourceBlockInfo getBlock(ResourceBlockId blockId) {
        return toBlockInfo(loadBlock(blockId));
    }

    public List<ResourceBlockInfo> findBlocksByResource(ResourceId resourceId) {
        return blocks.findByResourceId(resourceId).stream().map(this::toBlockInfo).toList();
    }

    public List<ResourceBlockInfo> findAllBlocks() {
        return blocks.findAll().stream().map(this::toBlockInfo).toList();
    }

    @Override
    public List<ResourceBlockInfo> findActiveBlocksOverlapping(Collection<UUID> resourceIds,
            ReservationPeriod period) {
        return blocksOverlapping(
                resourceIds.stream().map(ResourceId::of).toList(), period);
    }

    private List<ResourceBlockInfo> blocksOverlapping(Collection<ResourceId> typedResourceIds,
            ReservationPeriod period) {
        return blocks.findActiveOverlapping(typedResourceIds, period).stream().map(this::toBlockInfo).toList();
    }


    public void cancelBlock(ResourceBlockId blockId, CustomerId actor) {
        tx.run(() -> {
            ResourceBlock block = loadBlock(blockId);
            block.cancel(Instant.now(), actor);
            blocks.save(block);
        });
    }

    private Resource loadResource(ResourceId resourceId) {
        return resources.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private ResourceBlock loadBlock(ResourceBlockId blockId) {
        return blocks.findById(blockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOCK_NOT_FOUND));
    }

    private ResourceInfo toInfo(Resource resource) {
        return new ResourceInfo(
                resource.getId(), resource.getGroupId(), resource.getVenueId(), resource.getName(),
                resource.getCode(), resource.getType(), resource.getStatus());
    }

    private ResourceGroupInfo toGroupInfo(ResourceGroup group) {
        return new ResourceGroupInfo(group.getId(), group.getVenueId(), group.getName(), group.getType());
    }

    private ResourceBlockInfo toBlockInfo(ResourceBlock block) {
        return new ResourceBlockInfo(
                block.getId(), block.getResourceId(), block.getPeriod(), block.getReason(),
                block.getStatus(), block.getCreatedAt(), block.getCancelledAt(), block.getCancelledBy());
    }
}
