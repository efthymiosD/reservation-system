package com.decoupledx.reservation.shared.adapter.security;

import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class ValidatingJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final DelegatingOAuth2TokenValidator<Jwt> validators;

    ValidatingJwtDecoder(JwtDecoder delegate, List<OAuth2TokenValidator<Jwt>> validators) {
        this.delegate = delegate;
        this.validators = new DelegatingOAuth2TokenValidator<>(validators);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        OAuth2TokenValidatorResult result = validators.validate(jwt);
        if (result.hasErrors()) {
            String description = result.getErrors().stream()
                    .map(Object::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("invalid token");
            throw new BadJwtException(description);
        }
        return jwt;
    }
}
