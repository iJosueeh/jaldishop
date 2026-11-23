package com.jaldishop.backend.store.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StoreSettingsRequest(
        @NotBlank(message = "La política de cancelación no puede estar vacía")
        String cancellationPolicy,

        @NotBlank(message = "La política de reembolso no puede estar vacía")
        String refundPolicy,

        boolean requireCustomerDni,
        boolean allowOrderNotes,
        boolean onlinePaymentsEnabled,
        boolean cashOnDeliveryEnabled,
        boolean acceptsPosOnDelivery,

        @PositiveOrZero(message = "El monto mínimo no puede ser negativo")
        BigDecimal minDeliveryAmount,

        @Min(value = 5, message = "El tiempo de preparación debe ser de al menos 5 minutos")
        @Max(value = 480, message = "El tiempo de preparación no puede exceder 480 minutos")
        int estimatedPrepTimeMinutes,

        boolean orderSoundAlert,

        @Min(value = 0, message = "El volumen debe estar entre 0 y 100")
        @Max(value = 100, message = "El volumen debe estar entre 0 y 100")
        int soundVolume,

        boolean lowStockVisualAlert
) {
}
