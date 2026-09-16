package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OpeningHoursJpaRepository extends JpaRepository<OpeningHoursEntity, OpeningHoursEntityId> {

    List<OpeningHoursEntity> findByVenueId(UUID venueId);
}
