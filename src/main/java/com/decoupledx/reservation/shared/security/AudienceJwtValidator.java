package com.decoupledx.reservation.shared.security;

import java.util.Collection;

import lombok.NonNull;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error("invalid_token", "The aud claim is invalid or missing", null);

    private final String audience;

    @SuppressWarnings("NullableProblems")
    @Override
    public OAuth2TokenValidatorResult validate(@NonNull Jwt token) {
        Object aud = token.getClaim("aud");
        switch (aud) {
            case null -> {
                return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
            }
            case Collection<?> collection -> {
                if (collection.stream().anyMatch(audience::equals)) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
            case String s when audience.equals(s) -> {
                return OAuth2TokenValidatorResult.success();
            }
            default -> {
            }
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }

    AudienceJwtValidator(String audience) {
        this.audience = audience;
    }
}
