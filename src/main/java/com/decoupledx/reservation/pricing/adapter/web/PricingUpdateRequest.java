package com.decoupledx.reservation.pricing.adapter.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PricingUpdateRequest(
        @NotNull @DecimalMin("0.0") BigDecimal hourlyPrice,
        @NotBlank String currency) {
}
