package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ResourceBlockJpaRepository extends JpaRepository<ResourceBlockEntity, UUID> {

    List<ResourceBlockEntity> findByResourceId(UUID resourceId);

    @Query("""
            select b from ResourceBlockEntity b
            where b.resourceId in :resourceIds
              and b.status = 'ACTIVE'
              and b.startTime < :endTime
              and b.endTime > :startTime
            """)
    List<ResourceBlockEntity> findActiveOverlapping(
            @Param("resourceIds") Collection<UUID> resourceIds,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
