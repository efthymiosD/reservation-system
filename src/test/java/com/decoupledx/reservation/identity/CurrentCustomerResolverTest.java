package com.decoupledx.reservation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.decoupledx.reservation.identity.adapter.in.CurrentCustomerResolver;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

class CurrentCustomerResolverTest {

    private static final String API_SUB_1 = "api-sub-1";
    private final CustomerAccountService customerAccounts = mock(CustomerAccountService.class);
    private final CurrentCustomerResolver resolver = new CurrentCustomerResolver(customerAccounts);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesInternalCustomerFromOidcWebSession() {
        SecurityContextHolder.getContext()
                .setAuthentication(new OAuth2AuthenticationToken(
                        oidcUser("alice"), List.of(), "keycloak"));
        CustomerId customerId = CustomerId.random();
        when(customerAccounts.resolveOrProvision(any(), eq("alice"))).thenReturn(customerId);

        assertThat(resolver.currentCustomerId()).isEqualTo(customerId);
    }

    @Test
    void resolvesInternalCustomerFromJwtBearerToken() {
        SecurityContextHolder.getContext().setAuthentication(jwtTokenWithSubject());
        CustomerId customerId = CustomerId.random();
        when(customerAccounts.resolveOrProvision(any(), any())).thenReturn(customerId);

        assertThat(resolver.currentCustomerId()).isEqualTo(customerId);
    }

    @Test
    void rejectsPrincipalWithoutSubject() {
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(
                oidcUser(""), List.of(), "keycloak"));

        assertThatThrownBy(resolver::currentCustomerId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "anon", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(resolver::currentCustomerId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated customer principal present");
    }

    private OidcUser oidcUser(String subject) {
        Map<String, Object> claims = Map.of(
                IdTokenClaimNames.SUB, subject,
                StandardClaimNames.PREFERRED_USERNAME, subject);
        OidcIdToken idToken = new OidcIdToken(
                "test-token", Instant.now(), Instant.now().plusSeconds(60), claims);
        return new DefaultOidcUser(
                org.springframework.security.core.authority.AuthorityUtils
                        .createAuthorityList("ROLE_CUSTOMER"),
                idToken, IdTokenClaimNames.SUB);
    }

    private JwtAuthenticationToken jwtTokenWithSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(API_SUB_1)
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
