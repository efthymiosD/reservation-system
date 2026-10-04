package com.decoupledx.reservation.pricing.adapter.persistence;

import com.decoupledx.reservation.shared.Money;
import java.util.UUID;

/**
 * Persistence view of a pricing policy: the form {@link
 * com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository} uses internally.
 */
public record PricingPolicyDataValue(UUID venueId, Money hourlyPrice) {
}
