package com.decoupledx.reservation.resource.domain;

import com.decoupledx.reservation.resource.adapter.api.*;
import com.decoupledx.reservation.resource.adapter.persistence.ResourceDataValue;
import com.decoupledx.reservation.resource.adapter.persistence.ResourceGroupDataValue;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.resource.domain.port.ResourceService;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resources;
    private final ResourceGroupRepository groups;
    private final TransactionRunner tx;

    @Override
    public ResourceInfo getResource(ResourceId resourceId) {
        return toInfo(loadResource(resourceId));
    }

    @Override
    public List<ResourceInfo> findResources(VenueId venueId) {
        return resources.findByVenueId(venueId).stream().map(this::toInfo).toList();
    }

    @Override
    public List<ResourceInfo> findActiveResources(VenueId venueId) {
        return resources.findByVenueId(venueId).stream()
                .filter(data -> ResourceStatus.ACTIVE.name().equals(data.status()))
                .map(this::toInfo)
                .toList();
    }

    @Override
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
            resources.save(resource.toDataValue());
            return toInfo(resource.toDataValue());
        });
    }

    @Override
    public ResourceInfo activate(ResourceId resourceId) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.activate();
            return toInfo(resources.save(resource.toDataValue()));
        });
    }

    @Override
    public ResourceInfo deactivate(ResourceId resourceId) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.deactivate();
            return toInfo(resources.save(resource.toDataValue()));
        });
    }

    @Override
    public ResourceInfo rename(ResourceId resourceId, String newName) {
        return tx.run(() -> {
            Resource resource = loadResource(resourceId);
            resource.rename(newName);
            return toInfo(resources.save(resource.toDataValue()));
        });
    }

    @Override
    public ResourceInfo lockResource(ResourceId resourceId) {
        return tx.run(() -> toInfo(resources.lockById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }

    private Resource loadResource(ResourceId resourceId) {
        return toDomain(resources.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    private Resource toDomain(ResourceDataValue data) {
        return Resource.reconstitute(
                ResourceId.of(data.id()), ResourceGroupId.of(data.groupId()),
                VenueId.of(data.venueId()), data.name(), data.code(),
                ResourceType.valueOf(data.type()), ResourceStatus.valueOf(data.status()),
                data.createdAt(), data.updatedAt());
    }

    private ResourceInfo toInfo(Resource resource) {
        return toInfo(resource.toDataValue());
    }

    private ResourceInfo toInfo(ResourceDataValue data) {
        return new ResourceInfo(
                ResourceId.of(data.id()), ResourceGroupId.of(data.groupId()),
                VenueId.of(data.venueId()), data.name(), data.code(),
                ResourceType.valueOf(data.type()), ResourceStatus.valueOf(data.status()));
    }

    private ResourceGroupInfo toGroupInfo(ResourceGroupDataValue data) {
        return new ResourceGroupInfo(
                ResourceGroupId.of(data.id()), VenueId.of(data.venueId()),
                data.name(), ResourceType.valueOf(data.type()));
    }
}
