package com.decoupledx.reservation.webui;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-in user to every web UI view so templates stay free of
 * security dialects and controllers stay pure delegation.
 */
@ControllerAdvice
class CurrentUserAdvice {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @ModelAttribute("currentUser")
    CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return CurrentUser.ANONYMOUS;
        }
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
        return new CurrentUser(true, displayNameOf(authentication), administrator);
    }

    private String displayNameOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String preferredUsername = oidcUser.getPreferredUsername();
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return oidcUser.getName();
        }
        return authentication.getName();
    }
}
