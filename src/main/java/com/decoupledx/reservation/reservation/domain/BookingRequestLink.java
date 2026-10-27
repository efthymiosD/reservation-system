package com.decoupledx.reservation.reservation.domain;


import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestLinkDataValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One shareable booking-request link (MVP: a single reusable link at a time).
 * Persists only the SHA-256 hash of the token — the raw token reaches the
 * coach exactly once, at creation/regeneration.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class BookingRequestLink {

    static final String DEFAULT_LABEL = "Booking request link";

    private UUID id;
    private String label;
    private String tokenHash;
    private Instant createdAt;
    private Instant revokedAt;

    static BookingRequestLink issue(String label, String tokenHash, Instant now) {
        BookingRequestLink link = new BookingRequestLink();
        link.id = UUID.randomUUID();
        link.label = label == null || label.isBlank() ? DEFAULT_LABEL : label.trim();
        link.tokenHash = tokenHash;
        link.createdAt = now;
        return link;
    }

    static BookingRequestLink from(BookingRequestLinkDataValue data) {
        BookingRequestLink link = new BookingRequestLink();
        link.id = data.id();
        link.label = data.label();
        link.tokenHash = data.tokenHash();
        link.createdAt = data.createdAt();
        link.revokedAt = data.revokedAt();
        return link;
    }

    BookingRequestLinkDataValue toDataValue() {
        return new BookingRequestLinkDataValue(id, label, tokenHash, createdAt, revokedAt);
    }

    boolean isActive() {
        return revokedAt == null;
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
