package com.decoupledx.reservation.content.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_content")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SiteContentEntity {

    @Id
    private String key;

    @Column(nullable = false)
    private String body;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    SiteContentEntity(String key, String body, Instant updatedAt) {
        this.key = key;
        this.body = body;
        this.updatedAt = updatedAt;
    }

    void updateFrom(String body, Instant updatedAt) {
        this.body = body;
        this.updatedAt = updatedAt;
    }
}