package com.decoupledx.reservation.webui.admin;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * View model for the /admin/pricing page: the current hourly rate and its
 * currency. The page only shows and edits the pricing policy; past reservation
 * amounts are immutable snapshots taken at booking time, so changing the rate
 * here never rewrites history.
 */
record AdminPricingModel(String currencyCode, BigDecimal hourlyPrice, UUID venueId) {

    AdminPricingModel {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("A currency is required.");
        }
        if (hourlyPrice == null) {
            throw new IllegalArgumentException("A price is required.");
        }
    }
}
