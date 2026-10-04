package com.decoupledx.reservation.identity.domain.port;

import com.decoupledx.reservation.identity.adapter.api.CustomerEntry;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inbound port for the identity module: customer account resolution and provisioning.
 */
public interface CustomerAccountService {

    CustomerId resolveOrProvision(String idpSubject);

    CustomerId resolveOrProvision(String idpSubject, String displayName);

    Map<UUID, String> displayNames(Collection<CustomerId> customers);

    List<CustomerEntry> findAll();
}
