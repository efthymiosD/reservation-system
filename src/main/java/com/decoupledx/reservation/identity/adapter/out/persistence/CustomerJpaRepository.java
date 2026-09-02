package com.decoupledx.reservation.identity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByIdpSubject(String idpSubject);
}
