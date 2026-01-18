package com.rlnkoo.listingservice.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(
        BigDecimal amount,
        Currency currency
) {
    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(currencyCode, "CurrencyCode must not be null");
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }
}