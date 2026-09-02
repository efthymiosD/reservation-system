package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OpeningHoursJpaRepository extends JpaRepository<OpeningHoursEntity, OpeningHoursEntityId> {

    List<OpeningHoursEntity> findByVenueId(UUID venueId);

    @Modifying
    @Query("delete from OpeningHoursEntity o where o.venueId = :venueId")
    void deleteByVenueId(@Param("venueId") UUID venueId);
}
