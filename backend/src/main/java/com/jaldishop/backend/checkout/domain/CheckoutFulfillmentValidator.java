package com.jaldishop.backend.checkout.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.Store;

public final class CheckoutFulfillmentValidator {

    private CheckoutFulfillmentValidator() {
    }

    public static void validate(CheckoutFulfillmentType fulfillmentType, String deliveryAddress, Store store) {
        if (store == null) {
            throw new IllegalArgumentException("La tienda es obligatoria para validar el cumplimiento.");
        }
        if (fulfillmentType == null) {
            throw new BusinessRuleException("FULFILLMENT_TYPE_REQUIRED", "El tipo de entrega (PICKUP o DELIVERY) es obligatorio.");
        }

        if (fulfillmentType == CheckoutFulfillmentType.DELIVERY) {
            if (!store.isDeliveryEnabled()) {
                throw new ConflictException("DELIVERY_NOT_AVAILABLE", "La tienda no realiza envíos a domicilio.");
            }
            if (deliveryAddress == null || deliveryAddress.isBlank()) {
                throw new BusinessRuleException("DELIVERY_ADDRESS_REQUIRED", "La dirección de entrega es obligatoria para envíos a domicilio.");
            }
        } else if (fulfillmentType == CheckoutFulfillmentType.PICKUP) {
            if (!store.isPickupEnabled()) {
                throw new ConflictException("PICKUP_NOT_AVAILABLE", "La tienda no permite retiro en tienda.");
            }
        }
    }
}
