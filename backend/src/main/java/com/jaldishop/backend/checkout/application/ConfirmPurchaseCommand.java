package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmPurchaseCommand(
        UUID paymentId,
        CheckoutFulfillmentType fulfillmentType,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude
) {
    public ConfirmPurchaseCommand {
        if (paymentId == null) {
            throw new IllegalArgumentException("El ID del pago es obligatorio para confirmar la compra.");
        }
    }
}