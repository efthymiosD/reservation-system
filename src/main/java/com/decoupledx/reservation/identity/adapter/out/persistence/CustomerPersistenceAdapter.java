package com.decoupledx.reservation.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
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
    public CustomerId create(String idpSubject) {
        CustomerEntity entity = new CustomerEntity(
                UUID.randomUUID(), idpSubject, Instant.now());
        try {
            customers.saveAndFlush(entity);
            return CustomerId.of(entity.getCustomerId().toString());
        } catch (DataIntegrityViolationException conflict) {
            return findBySubject(idpSubject)
                    .orElseThrow(() -> conflict);
        }
    }
}
