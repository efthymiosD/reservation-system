package com.decoupledx.reservation.identity.adapter.in;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import com.decoupledx.reservation.identity.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

import lombok.RequiredArgsConstructor;

/**
 * Maps the authenticated principal's IdP subject to the app-owned internal
 * {@link CustomerId}. Accepts both principal types: {@link JwtAuthenticationToken}
 * (REST API bearer tokens) and an {@link OidcUser} principal inside an
 * OAuth2 session token (browser sessions via oauth2Login, where the user-name
 * attribute is configured as 'sub'). There is deliberately no preferred_username
 * fallback.
 */
@Service
@RequiredArgsConstructor
public class CurrentCustomerResolver implements CurrentCustomerApi {

    private final CustomerAccountService customerAccounts;

    public CustomerId currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return resolve(jwt.getName(), jwt.getToken().getClaimAsString("preferred_username"),
                    "JWT is missing the 'sub' claim required for customer identity");
        }
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return resolve(oidcUser.getName(), oidcUser.getPreferredUsername(),
                    "OIDC principal is missing the 'sub' claim required for customer identity");
        }
        throw new IllegalStateException("No authenticated customer principal present");
    }

    private CustomerId resolve(String subject, String displayName, String missingSubjectMessage) {
        if (subject != null && !subject.isBlank()) {
            return customerAccounts.resolveOrProvision(subject, displayName);
        }
        throw new IllegalStateException(missingSubjectMessage);
    }
}
