package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso para obtener contadores agregados de pedidos de una tienda.
 *
 * <p>Soporta:</p>
 * <ul>
 *   <li>Histórico completo de la tienda (cuando {@code serviceDate == null}).</li>
 *   <li>Fecha operativa específica (cuando {@code serviceDate != null}).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class GetMerchantOrderStatusCountsService {

    private final OrderRepository orderRepository;

    public GetMerchantOrderStatusCountsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public MerchantOrderStatusCountsResponse execute(UUID storeId, LocalDate serviceDate) {
        if (storeId == null) {
            throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");
        }

        OrderStatusCounts counts = orderRepository.countByStoreIdAndServiceDate(storeId, serviceDate);
        return MerchantOrderStatusCountsResponse.from(counts);
    }
}
