package com.decoupledx.reservation.reservation.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_request_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class BookingRequestLinkEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String label;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    BookingRequestLinkEntity(UUID id, String label, String tokenHash, Instant createdAt) {
        this.id = id;
        this.label = label;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
    }

    void applyRevocation(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
