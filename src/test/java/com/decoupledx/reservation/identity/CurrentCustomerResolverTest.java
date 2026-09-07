package com.decoupledx.reservation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.decoupledx.reservation.identity.adapter.in.CurrentCustomerResolver;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

class CurrentCustomerResolverTest {

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
                        oidcUserWithSubject("web-sub-1"), List.of(), "keycloak"));
        CustomerId customerId = CustomerId.random();
        when(customerAccounts.resolveOrProvision("web-sub-1")).thenReturn(customerId);

        assertThat(resolver.currentCustomerId()).isEqualTo(customerId);
    }

    @Test
    void resolvesInternalCustomerFromJwtBearerToken() {
        SecurityContextHolder.getContext().setAuthentication(jwtTokenWithSubject("api-sub-1"));
        CustomerId customerId = CustomerId.random();
        when(customerAccounts.resolveOrProvision("api-sub-1")).thenReturn(customerId);

        assertThat(resolver.currentCustomerId()).isEqualTo(customerId);
    }

    @Test
    void rejectsPrincipalWithoutSubject() {
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(
                oidcUserWithSubject(""), List.of(), "keycloak"));

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

    private OidcUser oidcUserWithSubject(String subject) {
        OidcUser user = mock(OidcUser.class);
        when(user.getName()).thenReturn(subject);
        return user;
    }

    private JwtAuthenticationToken jwtTokenWithSubject(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
