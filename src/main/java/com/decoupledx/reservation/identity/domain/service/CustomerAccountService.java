package com.decoupledx.reservation.identity.domain.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.decoupledx.reservation.identity.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomerAccountService implements CustomerDirectoryApi {

    private final CustomerAccountRepository customerAccounts;
    private final TransactionRunner tx;

    public CustomerId resolveOrProvision(String idpSubject) {
        return resolveOrProvision(idpSubject, null);
    }

    /**
     * Resolves the customer by IdP subject, provisioning on first login. The IdP
     * display name (preferred_username) is stored once on the row for admin
     * listings; existing rows are only seeded when the stored name is absent
     * (one-time update, not a per-request write).
     */
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

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException notAnId) {
            return false;
        }
    }
}
