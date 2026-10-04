package com.decoupledx.reservation.resource.adapter.persistence;

import com.decoupledx.reservation.resource.adapter.api.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.port.ResourceGroupRepository;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ResourceGroupPersistenceAdapter implements ResourceGroupRepository {

    private final ResourceGroupJpaRepository groups;

    @Override
    public Optional<ResourceGroupDataValue> findById(ResourceGroupId id) {
        return groups.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ResourceGroupDataValue> findByVenueId(VenueId venueId) {
        return groups.findByVenueId(venueId.value()).stream().map(this::toDomain).toList();
    }

    private ResourceGroupDataValue toDomain(ResourceGroupEntity entity) {
        return new ResourceGroupDataValue(entity.getId(), entity.getVenueId(), entity.getName(), entity.getType());
    }
}
