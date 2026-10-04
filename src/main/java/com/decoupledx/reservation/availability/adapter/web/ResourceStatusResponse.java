package com.decoupledx.reservation.availability.adapter.web;

import java.math.BigDecimal;
import java.util.UUID;

record ResourceStatusResponse(
        UUID resourceId,
        String name,
        String code,
        String type,
        String status,
        BigDecimal priceAmount,
        String priceCurrency) {
}
