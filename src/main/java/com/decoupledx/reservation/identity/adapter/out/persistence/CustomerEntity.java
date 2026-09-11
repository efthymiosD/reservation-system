package com.decoupledx.reservation.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CustomerEntity {

    @Id
    private UUID customerId;

    @Column(name = "idp_subject", nullable = false, unique = true)
    private String idpSubject;

    @Column(name = "display_name")
    @Setter
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    CustomerEntity(UUID customerId, String idpSubject, String displayName, Instant createdAt) {
        this.customerId = customerId;
        this.idpSubject = idpSubject;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }
}
