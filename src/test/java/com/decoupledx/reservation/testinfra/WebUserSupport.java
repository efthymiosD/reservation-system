package com.decoupledx.reservation.testinfra;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Builds an authenticated browser session for MockMvc in the exact shape the
 * production oauth2Login carries: an OidcUser resolved by 'sub' with a
 * preferred_username display name. The sub doubles as the JWT subject used by
 * {@link JwtSupport}, so the same internal CustomerId is resolved for both.
 */
public final class WebUserSupport {

    private WebUserSupport() {
    }

    public static RequestPostProcessor webUser(String subject) {
        Map<String, Object> claims = Map.of(
                IdTokenClaimNames.SUB, subject,
                StandardClaimNames.PREFERRED_USERNAME, subject);
        OidcIdToken idToken = new OidcIdToken(
                "test-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        OidcUser user = new DefaultOidcUser(
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"), idToken, IdTokenClaimNames.SUB);
        return SecurityMockMvcRequestPostProcessors.oidcLogin().oidcUser(user);
    }
}
