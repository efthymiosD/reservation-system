package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ResourceGroupJpaRepository extends JpaRepository<ResourceGroupEntity, UUID> {

    List<ResourceGroupEntity> findByVenueId(UUID venueId);
}
