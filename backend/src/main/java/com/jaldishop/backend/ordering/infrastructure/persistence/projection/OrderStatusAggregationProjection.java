package com.jaldishop.backend.ordering.infrastructure.persistence.projection;

/**
 * Proyección de agregación escalar para contadores de pedidos en base de datos.
 * Evita la instanciación de OrderEntity y la carga de relaciones EAGER.
 */
public interface OrderStatusAggregationProjection {
    long getTotal();
    long getConfirmed();
    long getInPreparation();
    long getReady();
    long getOutForDelivery();
    long getCompleted();
    long getCancelled();
}
