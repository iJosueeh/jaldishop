package com.jaldishop.backend.ordering.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Solicitud opcional de cancelación de comanda por parte del cliente.
 */
public record CancelOrderRequest(
        @Size(max = 255, message = "El motivo de cancelación no puede superar los 255 caracteres")
        String reason
) {}
