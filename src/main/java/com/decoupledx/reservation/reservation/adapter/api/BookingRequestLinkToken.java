package com.decoupledx.reservation.reservation.adapter.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque shareable token of a booking-request link: 128 bits of entropy,
 * URL-safe, no sequential data or personal data inside. Only
 * {@link #hashedValue()} (SHA-256, lowercase hex) is persisted — the raw
 * value reaches the coach exactly once per link creation/regeneration.
 */
public record BookingRequestLinkToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static BookingRequestLinkToken generate() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return new BookingRequestLinkToken(
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public String hashedValue() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException cannotHappen) {
            throw new IllegalStateException(cannotHappen);
        }
    }
}
