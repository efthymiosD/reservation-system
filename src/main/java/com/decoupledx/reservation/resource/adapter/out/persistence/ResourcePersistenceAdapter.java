package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.resource.domain.model.Resource;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ResourcePersistenceAdapter implements ResourceRepository {

    private final ResourceJpaRepository resources;

    @Override
    public Optional<Resource> findById(ResourceId id) {
        return resources.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Resource> lockById(ResourceId id) {
        return resources.lockById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Resource> findByVenueId(VenueId venueId) {
        return resources.findByVenueId(venueId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByVenueIdAndCode(VenueId venueId, String code) {
        return resources.existsByVenueIdAndCode(venueId.value(), code);
    }

    @Override
    public Resource save(Resource resource) {
        resources.findById(resource.getId().value())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(resource.getName(), resource.getStatus().name(), Instant.now()),
                        () -> resources.save(toEntity(resource)));
        return resource;
    }

    private ResourceEntity toEntity(Resource resource) {
        return new ResourceEntity(
                resource.getId().value(),
                resource.getGroupId().value(),
                resource.getVenueId().value(),
                resource.getName(),
                resource.getCode(),
                resource.getType().name(),
                resource.getStatus().name(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }

    private Resource toDomain(ResourceEntity entity) {
        return Resource.reconstitute(
                ResourceId.of(entity.getId()),
                ResourceGroupId.of(entity.getResourceGroupId()),
                VenueId.of(entity.getVenueId()),
                entity.getName(),
                entity.getCode(),
                ResourceType.valueOf(entity.getType()),
                ResourceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
