package com.decoupledx.reservation.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CustomerPersistenceAdapter implements CustomerAccountRepository {

    private final CustomerJpaRepository customers;

    @Override
    public Optional<CustomerId> findBySubject(String idpSubject) {
        return customers.findByIdpSubject(idpSubject)
                .map(entity -> CustomerId.of(entity.getCustomerId().toString()));
    }

    @Override
    public Map<CustomerId, String> displayNames(
            java.util.Collection<CustomerId> customerIds) {
        List<UUID> ids = customerIds.stream()
                .filter(id -> {
                    try {
                        UUID.fromString(id.value());
                        return true;
                    } catch (IllegalArgumentException notAnId) {
                        return false;
                    }
                })
                .map(id -> UUID.fromString(id.value()))
                .toList();
        return customers
                .findByCustomerIdIn(ids)
                .stream()
                .filter(entity -> entity.getDisplayName() != null)
                .collect(java.util.stream.Collectors.toMap(
                        entity -> CustomerId.of(entity.getCustomerId().toString()),
                        CustomerEntity::getDisplayName));
    }

    @Override
    public CustomerId create(String idpSubject) {
        return create(idpSubject, null);
    }

    @Override
    public void updateDisplayName(CustomerId customerId, String displayName) {
        customers.findFirstByCustomerId(UUID.fromString(customerId.value()))
                .ifPresent(entity -> {
                    entity.setDisplayName(displayName);
                    customers.save(entity);
                });
    }

    @Override
    public CustomerId create(String idpSubject, String displayName) {
        CustomerEntity entity = new CustomerEntity(
                UUID.randomUUID(), idpSubject, displayName, Instant.now());
        try {
            customers.saveAndFlush(entity);
            return CustomerId.of(entity.getCustomerId().toString());
        } catch (DataIntegrityViolationException conflict) {
            return findBySubject(idpSubject)
                    .orElseThrow(() -> conflict);
        }
    }
}
