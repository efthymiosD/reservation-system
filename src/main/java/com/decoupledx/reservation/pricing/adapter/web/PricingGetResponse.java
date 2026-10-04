package com.decoupledx.reservation.pricing.adapter.web;

import java.math.BigDecimal;

public record PricingGetResponse(BigDecimal hourlyPrice, String currency) {
}
