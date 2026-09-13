package com.decoupledx.reservation.administration.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecurringReservationJpaRepository extends JpaRepository<RecurringReservationEntity, UUID> {

    List<RecurringReservationEntity> findAllByOrderByCreatedAtDesc();

    List<RecurringReservationEntity> findByStatusOrderByCreatedAtDesc(String status);
}