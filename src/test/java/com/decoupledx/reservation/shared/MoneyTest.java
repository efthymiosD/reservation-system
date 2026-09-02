package com.decoupledx.reservation.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import com.decoupledx.reservation.shared.domain.Money;

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
}
