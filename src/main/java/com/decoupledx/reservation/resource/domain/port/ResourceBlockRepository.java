package com.decoupledx.reservation.resource.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.resource.domain.model.ResourceBlockId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceBlock;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

public interface ResourceBlockRepository {

    Optional<ResourceBlock> findById(ResourceBlockId id);

    List<ResourceBlock> findByResourceId(ResourceId resourceId);

    List<ResourceBlock> findAll();

    List<ResourceBlock> findActiveOverlapping(Collection<ResourceId> resourceIds, ReservationPeriod period);

    ResourceBlock save(ResourceBlock block);
}
