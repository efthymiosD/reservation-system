package com.decoupledx.reservation.identity.domain;

import com.decoupledx.reservation.identity.adapter.api.CustomerEntry;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import com.decoupledx.reservation.shared.TransactionRunner;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class CustomerAccountServiceImpl implements CustomerAccountService {

    private final CustomerAccountRepository customerAccounts;
    private final TransactionRunner tx;

    @Override
    public CustomerId resolveOrProvision(String idpSubject) {
        return resolveOrProvision(idpSubject, null);
    }

    @Override
    public CustomerId resolveOrProvision(String idpSubject, String displayName) {
        return tx.run(() -> {
            CustomerId existing = customerAccounts.findBySubject(idpSubject)
                    .orElseGet(() -> customerAccounts.create(idpSubject, displayName));
            if (displayName != null
                    && !customerAccounts.displayNames(java.util.List.of(existing))
                    .containsKey(existing)) {
                customerAccounts.updateDisplayName(existing, displayName);
            }
            return existing;
        });
    }

    @Override
    public Map<UUID, String> displayNames(Collection<CustomerId> customers) {
        return customerAccounts.displayNames(customers).entrySet().stream()
                .filter(entry -> isUuid(entry.getKey().value()))
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey().value()),
                        Map.Entry::getValue));
    }

    @Override
    public List<CustomerEntry> findAll() {
        return customerAccounts.findAll().stream()
                .filter(entry -> isUuid(entry.customerId().value()))
                .map(entry -> new CustomerEntry(
                        UUID.fromString(entry.customerId().value()),
                        entry.displayName()))
                .toList();
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException notAnId) {
            return false;
        }
    }
}
