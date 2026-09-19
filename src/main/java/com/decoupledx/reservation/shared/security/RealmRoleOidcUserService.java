package com.decoupledx.reservation.shared.security;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps Keycloak realm roles into session authorities for the OAuth2 login flow
 * (the browser counterpart of {@link JwtRolesConverter}): the realm-role
 * protocol mapper on the web client emits a flat multivalued 'roles' claim into
 * the ID token, and this service turns it into ROLE_* authorities so
 * /admin/** can be gated by ROLE_ADMIN.
 */
@Slf4j
class RealmRoleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(@NonNull OidcUserRequest request) {
        OidcUser user = delegate.loadUser(request);
        List<String> roles = user.getClaimAsStringList("roles");
        if (roles == null) {
            return user;
        }
        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.trim().toUpperCase(Locale.ROOT)));
        }
        return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "sub");
    }
}
