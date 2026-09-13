package com.decoupledx.reservation.identity.api;

import java.util.UUID;

/**
 * A provisioned customer as seen by admin-facing views: the internal customer
 * id plus the display name captured from the IdP (may be null for customers
 * with no stored name).
 */
public record CustomerEntry(UUID customerId, String displayName) {
}