package com.aicompass.compass.shared.money;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0 || amount.scale() > 6 || amount.compareTo(new BigDecimal("1000000000000")) >= 0
                || currency.getDefaultFractionDigits() < 0 || currency.getCurrencyCode().equals("XXX")) {
            throw new IllegalArgumentException("Money requires a nonnegative amount (up to 6 decimals) and a monetary currency");
        }
        amount = amount.stripTrailingZeros();
        if (amount.scale() < 0) amount = amount.setScale(0);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(Objects.requireNonNull(amount, "Unknown money cannot be zero")), Currency.getInstance(currency));
    }

    public Money add(Money other) {
        requireCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public boolean atMost(Money limit) {
        requireCurrency(limit);
        return amount.compareTo(limit.amount) <= 0;
    }

    public Money monthlyEquivalent(int months) {
        if (months < 1) throw new IllegalArgumentException("Positive period required");
        // Keep six decimals, rounding upward so a budget comparison cannot understate costs.
        return new Money(amount.divide(BigDecimal.valueOf(months), 6, RoundingMode.CEILING), currency);
    }

    public String decimalString() {
        return amount.toPlainString();
    }

    private void requireCurrency(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Explicit FX snapshot required for mixed currencies");
    }
}
