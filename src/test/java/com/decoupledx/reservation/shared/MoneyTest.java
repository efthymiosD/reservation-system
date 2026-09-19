package com.decoupledx.reservation.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency PLN = Currency.getInstance("PLN");

    @Test
    void normalizesAmountToTwoDecimalPlaces() {
        Money money = Money.of(new BigDecimal("10.005"), PLN);
        assertThat(money.amount()).isEqualByComparingTo("10.01");
    }

    @Test
    void multipliesByFactor() {
        Money hourly = Money.of(new BigDecimal("80"), PLN);
        Money result = hourly.multiply(new BigDecimal("1.5"));
        assertThat(result.amount()).isEqualByComparingTo("120.00");
        assertThat(result.currency()).isEqualTo(PLN);
    }

    @Test
    void addsSameCurrency() {
        Money sum = Money.of(new BigDecimal("80"), PLN).add(Money.of(new BigDecimal("40"), PLN));
        assertThat(sum.amount()).isEqualByComparingTo("120.00");
    }

    @Test
    void rejectsAdditionOfDifferentCurrencies() {
        Money pln = Money.of(new BigDecimal("80"), PLN);
        Money eur = Money.of(new BigDecimal("80"), Currency.getInstance("EUR"));
        assertThatThrownBy(() -> pln.add(eur)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> Money.of(null, PLN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void convertsToExactMinorUnits() {
        Money price = Money.of(new BigDecimal("80.00"), PLN);
        assertThat(price.minorUnits()).isEqualTo(8000L);
    }

    @Test
    void convertsWholeVenuePriceToMinorUnits() {
        assertThat(Money.of(new BigDecimal("80"), PLN).minorUnits()).isEqualTo(8000L);
    }

    @Test
    void convertsZeroToZeroMinorUnits() {
        assertThat(Money.of(new BigDecimal("0.00"), PLN).minorUnits()).isZero();
    }

    @Test
    void convertsCurrenciesWithNoFractionDigits() {
        Currency JPY = Currency.getInstance("JPY");
        assertThat(Money.of(new BigDecimal("100"), JPY).minorUnits()).isEqualTo(100L);
    }

    @Test
    void roundTripsThroughMinorUnits() {
        Money original = Money.of(new BigDecimal("47.35"), PLN);
        Money roundTripped = Money.ofMinorUnits(original.minorUnits(), original.currency());
        assertThat(roundTripped.amount()).isEqualByComparingTo("47.35");
        assertThat(roundTripped.currency()).isEqualTo(PLN);
    }

    @Test
    void constructsFromRawMinorUnits() {
        Money fromStripe = Money.ofMinorUnits(8000L, PLN);
        assertThat(fromStripe.amount()).isEqualByComparingTo("80.00");
        assertThat(fromStripe.currency()).isEqualTo(PLN);
    }
}
