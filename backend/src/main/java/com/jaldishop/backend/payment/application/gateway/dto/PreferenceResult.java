package com.jaldishop.backend.payment.application.gateway.dto;

import java.util.Objects;

/**
 * Resultado de la creación de una preferencia de Checkout Pro devuelto por el gateway.
 */
public record PreferenceResult(
        String preferenceId,
        String initPoint,
        String sandboxInitPoint
) {
    public PreferenceResult {
        Objects.requireNonNull(preferenceId, "preferenceId no puede ser nulo");
        Objects.requireNonNull(initPoint, "initPoint no puede ser nulo");
        if (preferenceId.isBlank()) {
            throw new IllegalArgumentException("preferenceId no puede estar en blanco");
        }
        if (initPoint.isBlank()) {
            throw new IllegalArgumentException("initPoint no puede estar en blanco");
        }
    }
}
