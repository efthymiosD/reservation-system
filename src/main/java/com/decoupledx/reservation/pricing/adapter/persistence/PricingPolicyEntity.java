package com.decoupledx.reservation.pricing.adapter.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pricing_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PricingPolicyEntity {

    @Id
    @Column(name = "venue_id")
    private UUID venueId;

    @Column(name = "hourly_price", nullable = false)
    private BigDecimal hourlyPrice;

    @Column(nullable = false)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    PricingPolicyEntity(UUID venueId, BigDecimal hourlyPrice, String currency, Instant updatedAt) {
        this.venueId = venueId;
        this.hourlyPrice = hourlyPrice;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    void updateFrom(BigDecimal hourlyPrice, String currency, Instant updatedAt) {
        this.hourlyPrice = hourlyPrice;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }
}
