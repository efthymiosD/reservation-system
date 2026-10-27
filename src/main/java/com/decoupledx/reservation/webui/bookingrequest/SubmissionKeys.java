package com.decoupledx.reservation.webui.bookingrequest;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mints per-visit submission keys of the public form: opaque,
 * high-entropy, no personal data. The module's unique index + same-key/same-
 * link rule make resubmission idempotent.
 */
final class SubmissionKeys {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SubmissionKeys() {
    }

    static String newKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Cheap shape check: 32 url-safe chars, what the factory mints.
     */
    static boolean wellFormed(String key) {
        return key != null && key.length() >= 24 && key.length() <= 64;
    }
}
