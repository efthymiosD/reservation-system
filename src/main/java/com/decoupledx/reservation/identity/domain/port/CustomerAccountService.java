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

    /**
     * Internal ids of the provisioned customers whose stored display name
     * EQUALS the given name (case-insensitive); several customers may share
     * a name. An empty or blank name yields an empty list — never an
     * unfiltered directory.
     */
    List<CustomerId> byExactName(String displayName);

    /**
     * Provisions (or reuses) an internal identity under a
     * workflow-namespaced subject: the booking-request approval path.
     */
    CustomerId provision(String idpSubject, String displayName);
}
