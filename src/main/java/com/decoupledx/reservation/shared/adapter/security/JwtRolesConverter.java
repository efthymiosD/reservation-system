package com.decoupledx.reservation.shared.adapter.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtRolesConverter implements Converter<Jwt, JwtAuthenticationToken> {

    static final String ROLES_CLAIM = "roles";
    static final String REALM_ACCESS_CLAIM = "realm_access";
    static final String RESOURCE_ACCESS_CLAIM = "resource_access";

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        roles.addAll(flatRoles(jwt));

        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        roles.addAll(nestedRoles(realmAccess));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess != null) {
            resourceAccess.values().forEach(entry ->
                    roles.addAll(nestedRoles(castToStringMap(entry))));
        }

        Collection<GrantedAuthority> authorities = roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private List<String> flatRoles(Jwt jwt) {
        if (!jwt.hasClaim(ROLES_CLAIM)) {
            return List.of();
        }
        try {
            return jwt.getClaimAsStringList(ROLES_CLAIM);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> nestedRoles(Map<String, Object> claim) {
        if (claim == null) {
            return List.of();
        }
        Object roles = claim.get(ROLES_CLAIM);
        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToStringMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }
}
