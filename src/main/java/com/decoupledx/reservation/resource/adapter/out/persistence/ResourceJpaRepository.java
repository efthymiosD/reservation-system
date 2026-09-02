package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface ResourceJpaRepository extends JpaRepository<ResourceEntity, UUID> {

    List<ResourceEntity> findByVenueId(UUID venueId);

    boolean existsByVenueIdAndCode(UUID venueId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResourceEntity r where r.id = :id")
    Optional<ResourceEntity> lockById(@Param("id") UUID id);
}
