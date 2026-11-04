package com.jaldishop.backend.payment.web.dto;

import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentPreferenceRequest(
        @NotNull(message = "El ID de la reserva de capacidad es obligatorio")
        UUID capacityReservationId,

        @NotNull(message = "El tipo de entrega es obligatorio")
        CheckoutFulfillmentType fulfillmentType,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String customerName,

        @NotBlank(message = "El teléfono del cliente es obligatorio")
        String customerPhone,

        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email debe ser válido")
        String customerEmail,

        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude
) {}
