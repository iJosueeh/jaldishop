package com.jaldishop.backend.checkout.web.dto;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record InitiateCheckoutRequest(
        @NotNull(message = "El storeId es obligatorio.")
        UUID storeId,

        @NotNull(message = "El tipo de entrega (PICKUP o DELIVERY) es obligatorio.")
        CheckoutFulfillmentType fulfillmentType,

        @NotNull(message = "La fecha de entrega/retiro es obligatoria.")
        @FutureOrPresent(message = "La fecha no puede ser anterior al día de hoy.")
        LocalDate serviceDate,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 200, message = "El nombre del cliente no puede exceder los 200 caracteres.")
        String customerName,

        @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres.")
        String customerPhone,

        @Size(max = 254, message = "El correo electrónico no puede exceder los 254 caracteres.")
        String customerEmail,

        @Size(max = 500, message = "La dirección de entrega no puede exceder los 500 caracteres.")
        String deliveryAddress,

        @Size(max = 300, message = "La referencia de entrega no puede exceder los 300 caracteres.")
        String deliveryReference,

        BigDecimal deliveryLatitude,

        BigDecimal deliveryLongitude,

        @Size(max = 80, message = "El código de descuento no puede exceder los 80 caracteres.")
        String discountCode
) {}
