package com.decoupledx.reservation.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.pricing.domain.model.PricingPolicy;

class PricingPolicyTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final PricingPolicy POLICY = new PricingPolicy(Money.of(new BigDecimal("80"), PLN));
    private static final Instant START = Instant.parse("2026-09-01T16:00:00Z");

    @Test
    void calculatesPriceForNinetyMinutes() {
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(START, Duration.ofMinutes(90));
        Money price = POLICY.calculatePrice(period);
        assertThat(price.amount()).isEqualByComparingTo("120.00");
        assertThat(price.currency()).isEqualTo(PLN);
    }

    @Test
    void calculatesPriceForOneHour() {
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(START, Duration.ofHours(1));
        assertThat(POLICY.calculatePrice(period).amount()).isEqualByComparingTo("80.00");
    }

    @Test
    void calculatesPriceForTwoHours() {
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(START, Duration.ofHours(2));
        assertThat(POLICY.calculatePrice(period).amount()).isEqualByComparingTo("160.00");
    }
}
