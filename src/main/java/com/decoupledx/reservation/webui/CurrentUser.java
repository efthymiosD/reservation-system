package com.decoupledx.reservation.webui;

import jakarta.annotation.Nullable;

/**
 * Presentation-only view of the signed-in user for the navigation bar.
 * displayName is the human-friendly claim (preferred_username) when available,
 * falling back to the opaque sub; email and customerId carry the session
 * principal claims (customerId is the internal identity, provisioned on first
 * use); administrator comes from the session authorities (ROLE_ADMIN), never
 * from client-side input.
 */
public record CurrentUser(
        boolean authenticated,
        String displayName,
        @Nullable String email,
        @Nullable String customerId,
        boolean administrator) {

    public static final CurrentUser ANONYMOUS = new CurrentUser(false, null, null, null, false);

    public static CurrentUser signedInAs(String displayName) {
        return new CurrentUser(true, displayName, null, null, false);
    }

    public static CurrentUser administrator(String displayName) {
        return new CurrentUser(true, displayName, null, null, true);
    }
}
