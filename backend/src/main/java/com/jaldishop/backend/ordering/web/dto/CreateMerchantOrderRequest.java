package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateMerchantOrderRequest(
        @NotNull(message = "La modalidad de entrega es obligatoria")
        DeliveryMode deliveryMode,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 200, message = "El nombre del cliente no puede superar los 200 caracteres")
        String customerName,

        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "El teléfono debe contener entre 9 y 15 dígitos numéricos")
        String customerPhone,

        @Email(message = "El formato del correo electrónico es inválido")
        String customerEmail,

        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal deliveryFeeAmount,

        @NotNull(message = "El método de pago es obligatorio")
        String paymentMethod,

        LocalDate serviceDate,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime,

        String notes,

        @NotEmpty(message = "El pedido debe contener al menos un producto")
        @Valid
        List<CreateMerchantOrderItemRequest> items
) {}
