package com.jaldishop.backend.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("Creación básica y normalización de decimales y divisa")
    void testBasicCreation() {
        Money m1 = Money.of(new BigDecimal("10.5"), "pen");
        assertEquals(new BigDecimal("10.50"), m1.amount());
        assertEquals("PEN", m1.currency());

        Money mDefault = Money.pen(100);
        assertEquals(new BigDecimal("100.00"), mDefault.amount());
        assertEquals("PEN", mDefault.currency());

        Money mZero = Money.zeroPen();
        assertTrue(mZero.isZero());
        assertEquals(new BigDecimal("0.00"), mZero.amount());
    }

    @Test
    @DisplayName("Operaciones aritméticas suma, resta y multiplicación")
    void testArithmeticOperations() {
        Money a = Money.pen(50.25);
        Money b = Money.pen(25.50);

        Money sum = a.add(b);
        assertEquals(new BigDecimal("75.75"), sum.amount());
        assertEquals("PEN", sum.currency());

        Money diff = a.subtract(b);
        assertEquals(new BigDecimal("24.75"), diff.amount());

        Money mult = a.multiply(2);
        assertEquals(new BigDecimal("100.50"), mult.amount());
    }

    @Test
    @DisplayName("Lanzar excepción al operar con monedas distintas")
    void testDifferentCurrenciesThrow() {
        Money soles = Money.of(100, "PEN");
        Money dolares = Money.of(100, "USD");

        assertThrows(IllegalArgumentException.class, () -> soles.add(dolares));
        assertThrows(IllegalArgumentException.class, () -> soles.subtract(dolares));
        assertThrows(IllegalArgumentException.class, () -> soles.compareTo(dolares));
    }

    @Test
    @DisplayName("Comparaciones y predicados")
    void testComparisons() {
        Money m1 = Money.pen(10.00);
        Money m2 = Money.pen(20.00);
        Money mNegative = Money.pen(-5.00);

        assertTrue(m2.isGreaterThan(m1));
        assertTrue(m1.isLessThan(m2));
        assertTrue(m1.isPositive());
        assertTrue(mNegative.isNegative());
        assertEquals("PEN 10.00", m1.toString());
    }

    @Test
    @DisplayName("Integración de MonetarySummary con Money")
    void testMonetarySummaryMoneyBridge() {
        MonetarySummary summary = MonetarySummary.of(
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                "DESC",
                new BigDecimal("5.00"),
                new BigDecimal("14.00"),
                new BigDecimal("95.00"),
                "PEN"
        );

        assertEquals(Money.pen(100.00), summary.subtotalMoney());
        assertEquals(Money.pen(10.00), summary.discountMoney());
        assertEquals(Money.pen(5.00), summary.deliveryFeeMoney());
        assertEquals(Money.pen(14.00), summary.taxMoney());
        assertEquals(Money.pen(95.00), summary.totalMoney());
        assertEquals(Money.pen(95.00), summary.calculateExpectedTotalMoney());
    }
}
