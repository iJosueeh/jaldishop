package com.jaldishop.backend.store.domain.model;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Value Object inmutable que modela las modalidades de entrega (retiro en tienda / despacho a domicilio),
 * tarifas aplicables y la tasa tributaria de la tienda.
 */
public record DeliveryConfiguration(
        boolean pickupEnabled,
        boolean deliveryEnabled,
        BigDecimal deliveryFeeAmount,
        String deliveryFeeCurrency,
        BigDecimal taxRate
) {
    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("18.00");
    public static final BigDecimal MAX_TAX_RATE = new BigDecimal("100");

    public DeliveryConfiguration {
        if (deliveryEnabled && deliveryFeeAmount != null && deliveryFeeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La tarifa de entrega no puede ser negativa.");
        }
        if (taxRate != null) {
            if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(MAX_TAX_RATE) > 0) {
                throw new IllegalArgumentException("La tasa de impuestos debe ser mayor o igual a 0 y menor o igual a 100.");
            }
        }
        deliveryFeeCurrency = (deliveryFeeCurrency != null && !deliveryFeeCurrency.isBlank())
                ? deliveryFeeCurrency.trim().toUpperCase(Locale.ROOT)
                : null;
    }

    public static DeliveryConfiguration of(
            boolean pickupEnabled,
            boolean deliveryEnabled,
            BigDecimal deliveryFeeAmount,
            String deliveryFeeCurrency,
            BigDecimal taxRate
    ) {
        return new DeliveryConfiguration(pickupEnabled, deliveryEnabled, deliveryFeeAmount, deliveryFeeCurrency, taxRate);
    }
}
