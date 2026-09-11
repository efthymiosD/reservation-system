package com.decoupledx.reservation.webui;

/**
 * Presentation-only view of the signed-in user for the navigation bar.
 * displayName is the human-friendly claim (preferred_username) when available,
 * falling back to the opaque sub. administrator comes from the session
 * authorities (ROLE_ADMIN), never from client-side input.
 */
public record CurrentUser(boolean authenticated, String displayName, boolean administrator) {

    public static final CurrentUser ANONYMOUS = new CurrentUser(false, null, false);

    public static CurrentUser signedInAs(String displayName) {
        return new CurrentUser(true, displayName, false);
    }

    public static CurrentUser administrator(String displayName) {
        return new CurrentUser(true, displayName, true);
    }
}
