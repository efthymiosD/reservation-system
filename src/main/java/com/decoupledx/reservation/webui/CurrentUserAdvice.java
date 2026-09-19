package com.decoupledx.reservation.webui;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;

import lombok.RequiredArgsConstructor;

/**
 * Exposes the signed-in user (display name, email and internal customer id) to
 * every web UI view so templates stay free of security dialects and controllers
 * stay pure delegation.
 */
@ControllerAdvice
@RequiredArgsConstructor
class CurrentUserAdvice {

    private final CurrentCustomerApi currentCustomer;

    @ModelAttribute("currentUser")
    CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return CurrentUser.ANONYMOUS;
        }
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        String email = emailOf(authentication);
        String customer = customerIdOrNull();
        return new CurrentUser(true, displayNameOf(authentication), email, customer, administrator);
    }

    private String customerIdOrNull() {
        try {
            return currentCustomer.currentCustomerId().value();
        } catch (RuntimeException unresolved) {
            return null;
        }
    }

    private String emailOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("email");
        }
        return null;
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
