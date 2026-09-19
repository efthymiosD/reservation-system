package com.decoupledx.reservation.resource.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.CreateResourceCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceGroupInfo;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;

import com.decoupledx.reservation.resource.domain.model.Resource;
import com.decoupledx.reservation.resource.domain.model.ResourceGroup;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueId;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceService implements ResourceApi {

    private final ResourceRepository resources;
    private final ResourceGroupRepository groups;
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
    public void activate(UUID resourceId) {
        activate(ResourceId.of(resourceId));
    }

    @Override
    public ResourceInfo deactivate(UUID resourceId) {
        return deactivate(ResourceId.of(resourceId));
    }

    @Override
    public void rename(UUID resourceId, String newName) {
        rename(ResourceId.of(resourceId), newName);
    }

    @Override
    public List<ResourceInfo> findActiveResources(UUID venueId) {
        return findActiveResources(VenueId.of(venueId));
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

    private Resource loadResource(ResourceId resourceId) {
        return resources.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private ResourceInfo toInfo(Resource resource) {
        return new ResourceInfo(
                resource.getId(), resource.getGroupId(), resource.getVenueId(), resource.getName(),
                resource.getCode(), resource.getType(), resource.getStatus());
    }

    private ResourceGroupInfo toGroupInfo(ResourceGroup group) {
        return new ResourceGroupInfo(group.getId(), group.getVenueId(), group.getName(), group.getType());
    }
}