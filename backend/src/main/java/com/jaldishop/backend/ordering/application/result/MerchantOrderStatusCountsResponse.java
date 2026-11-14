package com.jaldishop.backend.ordering.application.result;

import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;

/**
 * Contrato de respuesta para los contadores de pedidos del comerciante.
 *
 * <p>Estructura estrictamente coherente con:</p>
 * <ul>
 *   <li><strong>RN-COUNT-02</strong>: {@code all = confirmed + inPreparation + ready + outForDelivery + completed + cancelled}.</li>
 * </ul>
 */
public record MerchantOrderStatusCountsResponse(
        long all,
        long confirmed,
        long inPreparation,
        long ready,
        long outForDelivery,
        long completed,
        long cancelled
) {
    public static MerchantOrderStatusCountsResponse from(OrderStatusCounts counts) {
        if (counts == null) {
            return empty();
        }
        return new MerchantOrderStatusCountsResponse(
                counts.all(),
                counts.confirmed(),
                counts.inPreparation(),
                counts.ready(),
                counts.outForDelivery(),
                counts.completed(),
                counts.cancelled()
        );
    }

    public static MerchantOrderStatusCountsResponse empty() {
        return new MerchantOrderStatusCountsResponse(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
