package com.decoupledx.reservation.identity.domain;

import com.decoupledx.reservation.identity.adapter.api.CustomerEntry;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import com.decoupledx.reservation.shared.TransactionRunner;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class CustomerAccountServiceImpl implements CustomerAccountService {

    /**
     * Workflow-namespace of externally provisioned booker identities.
     */
    static final String EXTERNAL_SUBJECT_PREFIX = "wa-link:";

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

    @Override
    public List<CustomerId> byExactName(String displayName) {
        return customerAccounts.byExactName(displayName);
    }

    @Override
    public CustomerId provision(String idpSubject, String displayName) {
        if (idpSubject == null || !idpSubject.startsWith(EXTERNAL_SUBJECT_PREFIX)) {
            throw new IllegalArgumentException(
                    "external booker subjects must be workflow-namespaced (" + EXTERNAL_SUBJECT_PREFIX + "...)");
        }
        // resolveOrProvision already carries the tx + the seed-on-missing semantics.
        return resolveOrProvision(idpSubject, displayName);
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
