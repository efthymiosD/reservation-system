package com.decoupledx.reservation.shared.adapter.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.issuer-uri:}") String issuerUri,
            @Value("${app.security.audience:}") String audience,
            @Value("${app.security.dev-public-key:}") Resource devPublicKey,
            @Value("${app.security.allow-dev-key:true}") boolean allowDevKey) throws IOException {

        if (issuerUri != null && !issuerUri.isBlank()) {
            JwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
            List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
            validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
            if (audience != null && !audience.isBlank()) {
                validators.add(new AudienceJwtValidator(audience));
            }
            return new ValidatingJwtDecoder(decoder, validators);
        }
        if (!allowDevKey) {
            throw new IllegalStateException(
                    "No JWT issuer configured (app.security.issuer-uri); dev-key fallback disabled in this profile. "
                            + "Set JWT_ISSUER_URI.");
        }
        return NimbusJwtDecoder.withPublicKey(readPublicKey(devPublicKey)).build();
    }

    private static RSAPublicKey readPublicKey(Resource resource) throws IOException {
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "No JWT issuer configured (app.security.issuer-uri) and no dev public key available");
        }
        String pem;
        try (InputStream in = resource.getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid dev JWT public key", e);
        }
    }
}
