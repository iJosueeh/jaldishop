package com.jaldishop.backend.ordering.domain.model;

/**
 * Agregación inmutable de contadores de pedidos para una tienda.
 *
 * <p>Reglas de negocio garantizadas:</p>
 * <ul>
 *   <li><strong>RN-COUNT-01</strong>: Representa el historial completo o la fecha operativa especificada.</li>
 *   <li><strong>RN-COUNT-02</strong>: {@code all = confirmed + inPreparation + ready + outForDelivery + completed + cancelled}.
 *       No representa ventas netas ni ingresos.</li>
 *   <li><strong>RN-COUNT-03</strong>: Pedidos activos = {@code confirmed + inPreparation + ready}.</li>
 * </ul>
 */
public record OrderStatusCounts(
        long all,
        long confirmed,
        long inPreparation,
        long ready,
        long outForDelivery,
        long completed,
        long cancelled
) {
    public static OrderStatusCounts of(
            long confirmed,
            long inPreparation,
            long ready,
            long outForDelivery,
            long completed,
            long cancelled
    ) {
        long all = confirmed + inPreparation + ready + outForDelivery + completed + cancelled;
        return new OrderStatusCounts(all, confirmed, inPreparation, ready, outForDelivery, completed, cancelled);
    }

    public static OrderStatusCounts empty() {
        return new OrderStatusCounts(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    /**
     * Calcula los pedidos que demandan atención operativa en cocina/recojo (RN-COUNT-03).
     * Excluye OUT_FOR_DELIVERY, COMPLETED y CANCELLED.
     */
    public long activeOrdersCount() {
        return confirmed + inPreparation + ready;
    }
}
