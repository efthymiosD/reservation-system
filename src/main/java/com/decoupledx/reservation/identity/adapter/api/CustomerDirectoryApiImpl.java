package com.decoupledx.reservation.identity.adapter.api;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module external-facing facade implementing {@link CustomerDirectoryApi} on top of
 * the identity inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class CustomerDirectoryApiImpl implements CustomerDirectoryApi {

    private final CustomerAccountService customerAccountService;

    @Override
    public Map<UUID, String> displayNames(Collection<CustomerId> customers) {
        return customerAccountService.displayNames(customers);
    }

    @Override
    public List<CustomerEntry> findAll() {
        return customerAccountService.findAll();
    }

    @Override
    public List<CustomerId> idsByExactName(String displayName) {
        return customerAccountService.byExactName(displayName);
    }

    @Override
    public CustomerId provisionExternalCustomer(String idpSubject, String displayName) {
        return customerAccountService.provision(idpSubject, displayName);
    }
}
