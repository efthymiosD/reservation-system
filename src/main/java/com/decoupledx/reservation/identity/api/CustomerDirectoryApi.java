package com.decoupledx.reservation.identity.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module API of the identity module: display names captured from the IdP at
 * login time, keyed by internal customer id. Absent entries mean the customer
 * has no stored name yet.
 */
public interface CustomerDirectoryApi {

    Map<UUID, String> displayNames(Collection<CustomerId> customers);

    /**
     * All provisioned customers (internal id + display name), most-recently
     * created first. Used by the admin UI to pick a customer for a recurring
     * reservation.
     */
    List<CustomerEntry> findAll();
}