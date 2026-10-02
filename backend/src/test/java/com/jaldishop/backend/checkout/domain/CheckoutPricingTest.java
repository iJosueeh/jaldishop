package com.jaldishop.backend.checkout.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutPricingTest {

    @Test
    @DisplayName("Debe calcular precios correctamente para modalidad PICKUP (sin tarifa de envío)")
    void shouldCalculatePricingForPickup() {
        CheckoutItemSnapshot item1 = new CheckoutItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Pan", "Bolsa x10", "SKU1", null,
                2, new BigDecimal("10.00"), "PEN", new BigDecimal("20.00"), true
        );
        CheckoutItemSnapshot item2 = new CheckoutItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Pastel", "Unidad", "SKU2", null,
                1, new BigDecimal("15.00"), "PEN", new BigDecimal("15.00"), false
        );

        CheckoutPricing pricing = CheckoutPricing.calculate(
                List.of(item1, item2),
                CheckoutFulfillmentType.PICKUP,
                new BigDecimal("6.00"),
                new BigDecimal("18.00"),
                "PEN"
        );

        assertThat(pricing.productsSubtotalAmount()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(pricing.deliveryFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.taxRate()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(pricing.includedTaxAmount()).isEqualByComparingTo(new BigDecimal("5.34"));
        assertThat(pricing.totalAmount()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(pricing.currency()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Debe calcular precios correctamente para modalidad DELIVERY (incluyendo tarifa de envío)")
    void shouldCalculatePricingForDelivery() {
        CheckoutItemSnapshot item = new CheckoutItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Torta", "Porción", "SKU1", null,
                2, new BigDecimal("25.00"), "PEN", new BigDecimal("50.00"), true
        );

        CheckoutPricing pricing = CheckoutPricing.calculate(
                List.of(item),
                CheckoutFulfillmentType.DELIVERY,
                new BigDecimal("7.50"),
                new BigDecimal("18.00"),
                "PEN"
        );

        assertThat(pricing.productsSubtotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(pricing.deliveryFeeAmount()).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(pricing.totalAmount()).isEqualByComparingTo(new BigDecimal("57.50"));
        assertThat(pricing.includedTaxAmount()).isEqualByComparingTo(new BigDecimal("7.63"));
    }

    @Test
    @DisplayName("Debe manejar lista vacía y valores nulos por defecto")
    void shouldHandleEmptyListAndDefaults() {
        CheckoutPricing pricing = CheckoutPricing.calculate(
                List.of(),
                CheckoutFulfillmentType.PICKUP,
                null,
                null,
                null
        );

        assertThat(pricing.productsSubtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.deliveryFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.includedTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pricing.currency()).isEqualTo("PEN");
    }
}
