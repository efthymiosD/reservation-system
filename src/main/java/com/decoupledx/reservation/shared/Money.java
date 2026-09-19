package com.decoupledx.reservation.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Exact integer amount in the currency's minor units (e.g. PLN 80.00 → 8000
     * groszy). This is the loss-free seam payment providers such as Stripe
     * operate on (integer amounts + ISO-4217 code); it never rounds the stored
     * amount. {@code LongOverflowError} if it does not fit in a {@code long}.
     */
    public long minorUnits() {
        return amount
                .movePointRight(currency.getDefaultFractionDigits())
                .longValueExact();
    }

    /**
     * Inverse of {@link #minorUnits()}: reconstructs the canonical scale-2
     * amount from a provider-supplied integer minor-unit value.
     */
    public static Money ofMinorUnits(long minorUnits, Currency currency) {
        return new Money(BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits()), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot combine amounts in different currencies: %s and %s".formatted(currency, other.currency));
        }
    }
}
