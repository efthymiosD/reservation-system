package com.decoupledx.reservation.shared.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Single logout for the browser session: after the local logout, sends the user
 * to Keycloak's OIDC end-session endpoint (with the id token hint) so the SSO
 * session is terminated too — otherwise 'Log in' would re-authenticate silently.
 * Keycloak then redirects back to our /logged-out page.
 */
@Component
class KeycloakLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler
        implements LogoutSuccessHandler {

    private final String endSessionUri;

    KeycloakLogoutSuccessHandler(
            @Value("${app.security.end-session-uri}") String endSessionUri) {
        this.endSessionUri = endSessionUri;
    }

    @Override
    public void onLogoutSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                Authentication authentication) throws IOException {
        String postLogoutRedirectUri = baseUrlOf(request) + "/logged-out";
        String target = endSessionUri
                + "?post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8);
        String idTokenHint = idTokenHintOf(authentication);
        if (idTokenHint != null) {
            target += "&id_token_hint=" + URLEncoder.encode(idTokenHint, StandardCharsets.UTF_8);
        }
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private String idTokenHintOf(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getIdToken().getTokenValue();
        }
        return null;
    }

    private String baseUrlOf(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultPort = (request.isSecure() && port == 443) || (!request.isSecure() && port == 80);
        String authority = request.getServerName() + (defaultPort ? "" : ":" + port);
        return request.getScheme() + "://" + authority + request.getContextPath();
    }
}
