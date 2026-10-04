package com.decoupledx.reservation.resource.adapter.persistence;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.domain.port.ResourceRepository;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ResourcePersistenceAdapter implements ResourceRepository {

    private final ResourceJpaRepository resources;

    @Override
    public Optional<ResourceDataValue> findById(ResourceId id) {
        return resources.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<ResourceDataValue> lockById(ResourceId id) {
        return resources.lockById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ResourceDataValue> findByVenueId(VenueId venueId) {
        return resources.findByVenueId(venueId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByVenueIdAndCode(VenueId venueId, String code) {
        return resources.existsByVenueIdAndCode(venueId.value(), code);
    }

    @Override
    public ResourceDataValue save(ResourceDataValue resource) {
        resources.findById(resource.id()).ifPresentOrElse(
                entity -> entity.updateFrom(resource.name(), resource.status(), Instant.now()),
                () -> resources.save(toEntity(resource)));
        return resource;
    }

    private ResourceEntity toEntity(ResourceDataValue data) {
        return new ResourceEntity(
                data.id(), data.groupId(), data.venueId(), data.name(), data.code(),
                data.type(), data.status(), data.createdAt(), data.updatedAt());
    }

    private ResourceDataValue toDomain(ResourceEntity entity) {
        return new ResourceDataValue(
                entity.getId(), entity.getResourceGroupId(), entity.getVenueId(),
                entity.getName(), entity.getCode(), entity.getType(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
