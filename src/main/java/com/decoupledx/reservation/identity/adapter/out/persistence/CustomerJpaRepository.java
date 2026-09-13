package com.decoupledx.reservation.identity.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByIdpSubject(String idpSubject);

    List<CustomerEntity> findByCustomerIdIn(Collection<UUID> customerIds);

    List<CustomerEntity> findAllByOrderByCreatedAtDesc();

    Optional<CustomerEntity> findFirstByCustomerId(UUID customerId);
}
