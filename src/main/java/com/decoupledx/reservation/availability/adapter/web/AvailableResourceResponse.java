package com.decoupledx.reservation.availability.adapter.web;

import java.math.BigDecimal;
import java.util.UUID;

record AvailableResourceResponse(
        UUID resourceId,
        String name,
        String code,
        String type,
        BigDecimal priceAmount,
        String priceCurrency) {
}
