package com.decoupledx.reservation.identity.domain.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.decoupledx.reservation.identity.api.CustomerId;

public interface CustomerAccountRepository {

    Optional<CustomerId> findBySubject(String idpSubject);

    CustomerId create(String idpSubject);

    CustomerId create(String idpSubject, String displayName);

    /**
     * Display names captured at login time, looked up by internal customer id.
     * Customers without a stored name are absent from the result.
     */
    Map<CustomerId, String> displayNames(Collection<CustomerId> customerIds);

    /**
     * Seeds/overwrites the display name for an already-provisioned customer.
     */
    void updateDisplayName(CustomerId customerId, String displayName);
}
