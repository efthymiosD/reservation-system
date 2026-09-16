package com.decoupledx.reservation.content.domain.model;

import java.time.Instant;
import java.util.Objects;

import lombok.Getter;

/**
 * Aggregate root for a piece of editable site content: a stable key and its
 * body text (or, for the hero photo key, the web path of the uploaded image).
 */
@Getter
public class SiteText {

    private final String key;
    private String body;
    private Instant updatedAt;

    private SiteText(String key, String body, Instant updatedAt) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SiteText create(String key, String body, Instant now) {
        return new SiteText(key, body, now);
    }

    public static SiteText reconstitute(String key, String body, Instant updatedAt) {
        return new SiteText(key, body, updatedAt);
    }

    public void updateBody(String body, Instant now) {
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }
}