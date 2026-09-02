package com.decoupledx.reservation.reservation.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    List<ReservationEntity> findByCustomerIdOrderByStartTimeDesc(String customerId);

    Page<ReservationEntity> findByCustomerIdOrderByStartTimeDesc(String customerId, Pageable pageable);

    Page<ReservationEntity> findByCustomerIdAndStatusOrderByStartTimeDesc(String customerId, String status, Pageable pageable);

    long countByCustomerIdAndStatus(String customerId, String status);

    long countByCustomerId(String customerId);

    Page<ReservationEntity> findByStatusOrderByStartTimeDesc(String status, Pageable pageable);

    Page<ReservationEntity> findAllByOrderByStartTimeDesc(Pageable pageable);

    long countByStatus(String status);

    @Query("""
            select r from ReservationEntity r
            where r.resourceId = :resourceId
              and r.status = 'ACTIVE'
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    List<ReservationEntity> findActiveOverlappingResource(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("""
            select case when count(r) > 0 then true else false end from ReservationEntity r
            where r.customerId = :customerId
              and r.status = 'ACTIVE'
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsActiveOverlappingCustomer(
            @Param("customerId") String customerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
