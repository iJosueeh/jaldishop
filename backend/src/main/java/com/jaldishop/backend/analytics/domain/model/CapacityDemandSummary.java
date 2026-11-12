package com.jaldishop.backend.analytics.domain.model;

/**
 * Resumen de demanda de cupos de capacidad horaria.
 * <p>
 * NOTA DE ARQUITECTURA: {@code committedReservationsCount} mide cupos comprometidos durante la
 * confirmación de pedidos. No garantiza atención culinaria o entrega efectiva.
 * </p>
 */
public record CapacityDemandSummary(
        long committedReservationsCount,
        long releasedReservationsCount,
        String notes
) {
    public static final String DEFAULT_NOTES =
            "Demanda de cupos comprometidos (COMMITTED). No equivale a servicios efectivamente atendidos.";

    public static CapacityDemandSummary of(long committed, long released) {
        return new CapacityDemandSummary(committed, released, DEFAULT_NOTES);
    }
}
