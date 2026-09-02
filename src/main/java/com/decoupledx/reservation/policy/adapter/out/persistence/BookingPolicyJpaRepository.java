package com.decoupledx.reservation.policy.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingPolicyJpaRepository extends JpaRepository<BookingPolicyEntity, UUID> {
}
