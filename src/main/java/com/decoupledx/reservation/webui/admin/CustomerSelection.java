package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import java.util.List;

/**
 * The customer constraint of the admin reservations listing, resolved from a
 * raw form value. Picker semantics: the submitted value must be an EXISTING
 * customer display name (equality, case-insensitive); anything else — blank,
 * typed freely — yields an explicit no-match rather than an unfiltered
 * listing. When several customers share the exact name, all of them match.
 */
record CustomerSelection(String selectedName, List<CustomerId> matchedCustomers) {

    static CustomerSelection unspecified() {
        return new CustomerSelection(null, List.of());
    }

    static CustomerSelection resolve(CustomerDirectoryApi directory, String typedValue) {
        if (typedValue == null || typedValue.isBlank()) {
            return unspecified();
        }
        String name = typedValue.trim();
        return new CustomerSelection(name, directory.idsByExactName(name));
    }

    boolean unconstrained() {
        return selectedName == null;
    }

    boolean isNoMatch() {
        return !unconstrained() && matchedCustomers.isEmpty();
    }

    List<CustomerId> asFilterConstraint() {
        return matchedCustomers;
    }
}
