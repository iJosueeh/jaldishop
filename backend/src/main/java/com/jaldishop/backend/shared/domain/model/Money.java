package com.jaldishop.backend.shared.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    public static final String DEFAULT_CURRENCY = "PEN";
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public Money {
        BigDecimal effectiveAmount = amount != null ? amount : BigDecimal.ZERO;
        effectiveAmount = effectiveAmount.setScale(SCALE, ROUNDING_MODE);
        String effectiveCurrency = (currency != null && !currency.isBlank())
                ? currency.trim().toUpperCase(Locale.ROOT)
                : DEFAULT_CURRENCY;

        amount = effectiveAmount;
        currency = effectiveCurrency;
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money pen(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money pen(double amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroPen() {
        return zero(DEFAULT_CURRENCY);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "El monto a sumar no puede ser nulo.");
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "El monto a restar no puede ser nulo.");
        ensureSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "El factor multiplicador no puede ser nulo.");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money multiply(int factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    private void ensureSameCurrency(Money other) {
        if (!this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException(String.format(
                    "No se pueden operar importes con distintas divisas: %s vs %s",
                    this.currency, other.currency
            ));
        }
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "El monto a comparar no puede ser nulo.");
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }
}
