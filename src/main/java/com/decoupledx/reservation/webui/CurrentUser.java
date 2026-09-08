package com.decoupledx.reservation.webui;

/**
 * Presentation-only view of the signed-in user for the navigation bar.
 * displayName is the human-friendly claim (preferred_username) when available,
 * falling back to the opaque sub.
 */
public record CurrentUser(boolean authenticated, String displayName) {

    public static final CurrentUser ANONYMOUS = new CurrentUser(false, null);

    public static CurrentUser signedInAs(String displayName) {
        return new CurrentUser(true, displayName);
    }
}
