package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para actualizar el estado de un pedido por parte del comerciante.
 */
public record ChangeOrderStatusRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        OrderStatus status,

        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String reason
) {
    public ChangeOrderStatusRequest(OrderStatus status) {
        this(status, null);
    }
}
