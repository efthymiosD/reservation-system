package com.decoupledx.reservation.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Wraps the default authorization-request resolver and always adds login
 * hints to the Keycloak redirect:
 *
 * <ul>
 *   <li>{@code prompt=login}, so a stale Keycloak SSO cookie must never
 *       silently re-authenticate the previous user when someone else tries
 *       to sign in;</li>
 *   <li>{@code ui_locales}, taken from the app's own {@code locale} cookie
 *       (same name as {@code webui/LocaleConfig}'s
 *       {@link org.springframework.web.servlet.i18n.CookieLocaleResolver} —
 *       referenced by name only to keep the identity/security seam outside
 *       the webui module), so the Keycloak login screen opens in the
 *       language the visitor chose on the site.</li>
 * </ul>
 *
 * <p>The app cookie is the single source of truth for language: a switcher
 * inside the Keycloak screen is session-local there and is intentionally
 * NOT propagated back into the app cookie.</p>
 */
class PromptLoginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

    public PromptLoginAuthorizationRequestResolver(ClientRegistrationRepository clients,
                                                   String authorizationBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clients, authorizationBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(@NonNull HttpServletRequest request) {
        return withLoginHints(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(@NonNull HttpServletRequest request, @NonNull String clientRegistrationId) {
        return withLoginHints(delegate.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest withLoginHints(OAuth2AuthorizationRequest request, HttpServletRequest origin) {
        if (request == null) {
            return null;
        }
        String localeTag = appLocaleTag(origin);
        return OAuth2AuthorizationRequest.from(request)
                .parameters(params -> {
                    params.put("prompt", "login");
                    if (localeTag != null) {
                        params.put("ui_locales", localeTag);
                    }
                })
                .build();
    }

    /** The visitor's persisted choice, or {@code null} when absent/unusable. */
    private static String appLocaleTag(HttpServletRequest origin) {
        Cookie[] cookies = origin.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("locale".equals(cookie.getName())) {
                Locale locale = Locale.forLanguageTag(cookie.getValue());
                return locale.getLanguage().isBlank() ? null : locale.toString();
            }
        }
        return null;
    }
}
