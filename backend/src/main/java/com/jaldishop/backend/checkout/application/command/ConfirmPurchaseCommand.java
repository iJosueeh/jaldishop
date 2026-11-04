package com.jaldishop.backend.checkout.application.command;

import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;

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
    public ConfirmPurchaseCommand(UUID paymentId) {
        this(paymentId, null, null, null, null, null, null, null, null);
    }

    public ConfirmPurchaseCommand {
        if (paymentId == null) {
            throw new IllegalArgumentException("El ID del pago es obligatorio para confirmar la compra.");
        }
    }
}
