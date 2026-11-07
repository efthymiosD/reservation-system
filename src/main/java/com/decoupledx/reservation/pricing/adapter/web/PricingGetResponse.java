package com.decoupledx.reservation.pricing.adapter.web;

import java.math.BigDecimal;

record PricingGetResponse(BigDecimal hourlyPrice, String currency) {
}
