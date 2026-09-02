package com.decoupledx.reservation.reservation.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReservationEntity {

    @Id
    private UUID id;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false)
    private String status;

    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false)
    private String priceCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Version
    private long version;

    ReservationEntity(UUID id, UUID resourceId, String customerId, Instant startTime, Instant endTime,
                      String status, BigDecimal priceAmount, String priceCurrency,
                      Instant createdAt, Instant cancelledAt, String cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.customerId = customerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    void updateFrom(String status, Instant cancelledAt, String cancelledBy) {
        this.status = status;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }
}
