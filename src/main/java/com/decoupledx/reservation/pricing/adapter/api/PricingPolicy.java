package com.decoupledx.reservation.pricing.adapter.api;

import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record PricingPolicy(Money hourlyPrice) {

    public PricingPolicy {
        Objects.requireNonNull(hourlyPrice, "hourlyPrice must not be null");
    }

    public Money calculatePrice(ReservationPeriod period) {
        BigDecimal hours = BigDecimal.valueOf(period.duration().toMinutes())
                .divide(BigDecimal.valueOf(60), 10, RoundingMode.HALF_UP);
        return hourlyPrice.multiply(hours);
    }
}
