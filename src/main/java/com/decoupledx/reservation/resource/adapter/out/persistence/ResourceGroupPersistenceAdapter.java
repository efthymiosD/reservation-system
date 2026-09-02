package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.resource.domain.model.ResourceGroup;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ResourceGroupPersistenceAdapter implements ResourceGroupRepository {

    private final ResourceGroupJpaRepository groups;

    @Override
    public Optional<ResourceGroup> findById(ResourceGroupId id) {
        return groups.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ResourceGroup> findByVenueId(VenueId venueId) {
        return groups.findByVenueId(venueId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public ResourceGroup save(ResourceGroup group) {
        groups.findById(group.getId().value())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(group.getName(), Instant.now()),
                        () -> groups.save(toEntity(group)));
        return group;
    }

    private ResourceGroupEntity toEntity(ResourceGroup group) {
        return new ResourceGroupEntity(
                group.getId().value(),
                group.getVenueId().value(),
                group.getName(),
                group.getType().name(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }

    private ResourceGroup toDomain(ResourceGroupEntity entity) {
        return ResourceGroup.reconstitute(
                ResourceGroupId.of(entity.getId()),
                VenueId.of(entity.getVenueId()),
                entity.getName(),
                ResourceType.valueOf(entity.getType()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
