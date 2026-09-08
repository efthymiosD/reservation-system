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

    @ModelAttribute("currentUser")
    CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return CurrentUser.ANONYMOUS;
        }
        return new CurrentUser(true, displayNameOf(authentication));
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
