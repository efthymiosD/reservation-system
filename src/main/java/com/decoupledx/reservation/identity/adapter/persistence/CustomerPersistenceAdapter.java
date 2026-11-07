package com.decoupledx.reservation.identity.adapter.persistence;

import com.decoupledx.reservation.identity.adapter.api.CustomerAccountInfo;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
    public List<CustomerAccountInfo> findAll() {
        return customers.findAllByOrderByCreatedAtDesc().stream()
                .map(entity -> new CustomerAccountInfo(
                        CustomerId.of(entity.getCustomerId().toString()),
                        entity.getDisplayName()))
                .toList();
    }

    @Override
    public List<CustomerId> byExactName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return List.of();
        }
        return customers.findByDisplayNameIgnoreCase(displayName.trim()).stream()
                .map(entity -> CustomerId.of(entity.getCustomerId().toString()))
                .toList();
    }


    @Override
    public CustomerId create(String idpSubject, String displayName) {
        try {
            CustomerEntity entity = new CustomerEntity(
                    UUID.randomUUID(), idpSubject, displayName, Instant.now());
            customers.saveAndFlush(entity);
            return CustomerId.of(entity.getCustomerId().toString());
        } catch (DataIntegrityViolationException provisioningRace) {
            // 2026-11-07 aborted-transaction rule: this recovery re-read runs
            // OUTSIDE any wrapped tx (each repository call is its own unit).
            // The loser seeds its own name onto a nameless winner only. Note
            // a nameless row can also exist pre-deploy (first login without a
            // preferred_username claim) — such rows are no longer auto-named
            // by later logins (never probe per request); a backfill, if ever
            // wanted, must be a one-off, not request-path logic.
            CustomerEntity winner = customers.findByIdpSubject(idpSubject)
                    .orElseThrow(() -> provisioningRace);
            if (winner.getDisplayName() == null && displayName != null) {
                winner.setDisplayName(displayName);
                customers.save(winner);
            }
            return CustomerId.of(winner.getCustomerId().toString());
        }
    }
}
