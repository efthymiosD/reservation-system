package com.decoupledx.reservation.content.adapter.api;

/**
 * A single editable site-content block: a stable key plus its current body.
 * Keys are seeded in V2__seed_data.sql; a stable key set keeps the design
 * i18n-ready (a locale can be added later without touching call sites).
 */
public record SiteContentBlock(String key, String body) {
}