package com.decoupledx.reservation.shared.adapter.security;

import java.util.Collection;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error("invalid_token", "The aud claim is invalid or missing", null);

    private final String audience;

    AudienceJwtValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object aud = token.getClaim("aud");
        if (aud == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }
        if (aud instanceof Collection<?> collection) {
            if (collection.stream().anyMatch(audience::equals)) {
                return OAuth2TokenValidatorResult.success();
            }
        } else if (aud instanceof String s && audience.equals(s)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
