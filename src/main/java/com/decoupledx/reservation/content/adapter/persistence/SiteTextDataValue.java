package com.decoupledx.reservation.content.adapter.persistence;

import java.time.Instant;

/**
 * Persistence view of a site-text block.
 */
public record SiteTextDataValue(String key, String body, Instant updatedAt) {
}
