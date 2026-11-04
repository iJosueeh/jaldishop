package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetMerchantOrderDetailService {

    private final OrderRepository orderRepository;

    public GetMerchantOrderDetailService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public MerchantOrderDetailResponse execute(UUID orderId, UUID storeId) {
        if (orderId == null) {
            throw new IllegalArgumentException("El identificador del pedido es obligatorio.");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");
        }

        Order order = orderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        return MerchantOrderDetailResponse.from(order);
    }
}
