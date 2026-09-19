package com.decoupledx.reservation.shared.security;

import jakarta.servlet.http.HttpServletRequest;

import lombok.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Wraps the default authorization-request resolver and always adds
 * {@code prompt=login}: a stale Keycloak SSO cookie must never silently
 * re-authenticate the previous user when someone else tries to sign in.
 */
class PromptLoginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

    public PromptLoginAuthorizationRequestResolver(ClientRegistrationRepository clients,
                                                   String authorizationBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clients, authorizationBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(@NonNull HttpServletRequest request) {
        return withPromptLogin(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(@NonNull HttpServletRequest request, @NonNull String clientRegistrationId) {
        return withPromptLogin(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest withPromptLogin(OAuth2AuthorizationRequest request) {
        return request == null ? null : OAuth2AuthorizationRequest.from(request)
                .parameters(params -> params.put("prompt", "login"))
                .build();
    }
}
