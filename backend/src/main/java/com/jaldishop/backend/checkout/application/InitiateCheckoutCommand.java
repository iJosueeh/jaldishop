package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record InitiateCheckoutCommand(
        UUID userId,
        UUID storeId,
        CheckoutFulfillmentType fulfillmentType,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String discountCode
) {
    public InitiateCheckoutCommand {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para iniciar el checkout.");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio para iniciar el checkout.");
        }
        if (serviceDate == null) {
            throw new BusinessRuleException("SERVICE_DATE_REQUIRED", "La fecha de servicio es obligatoria.");
        }
    }
}
