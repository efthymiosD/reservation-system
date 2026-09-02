package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VenueJpaRepository extends JpaRepository<VenueEntity, UUID> {
}
