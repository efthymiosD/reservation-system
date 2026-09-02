package com.decoupledx.reservation.pricing.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PricingPolicyJpaRepository extends JpaRepository<PricingPolicyEntity, UUID> {
}
