package com.decoupledx.reservation.identity.adapter.api;

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

    /**
     * Internal ids of all provisioned customers whose stored display name
     * EQUALS the given name (case-insensitive); several customers may share
     * a name. An empty or blank name yields an empty list — never an
     * unfiltered directory. Used by the admin filters to translate a picked
     * customer name into ids.
     */
    List<CustomerId> idsByExactName(String displayName);

    /**
     * Provisions (or reuses) an internal customer identity for an externally named booker
     * WITHOUT an IdP session — the booking-request workflow's approval path.
     * The subject is a workflow-namespaced opaque id (e.g.
     * {@code wa-link:<…>}), never a real IdP subject.
     */
    CustomerId provisionExternalCustomer(String idpSubject, String displayName);
}