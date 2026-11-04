package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud HTTP para actualizar el estado de un pedido por parte del comerciante.
 */
public record ChangeOrderStatusRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        OrderStatus status
) {}
