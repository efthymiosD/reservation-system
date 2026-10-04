package com.decoupledx.reservation.identity.adapter.api;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

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
}
