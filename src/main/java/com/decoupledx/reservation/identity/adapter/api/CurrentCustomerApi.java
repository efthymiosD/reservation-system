package com.decoupledx.reservation.identity.adapter.api;

/**
 * Module API of the identity module: resolves the authenticated principal's IdP
 * subject to the app-owned internal customer identity. This is the only type
 * other modules may depend on.
 */
public interface CurrentCustomerApi {

    CustomerId currentCustomerId();

    /**
     * Whether the current principal acts as an administrator. Booking and
     * other use cases may relax current-player invariants for admins (the
     * module context is the only place allowed to look at roles).
     */
    boolean isAdministrator();
}
