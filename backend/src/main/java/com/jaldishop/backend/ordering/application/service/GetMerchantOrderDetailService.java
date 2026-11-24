package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.application.port.out.OrderFeePolicyPort;
import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class GetMerchantOrderDetailService {

    private final OrderRepository orderRepository;
    private final OrderFeePolicyPort orderFeePolicyPort;

    @Autowired
    public GetMerchantOrderDetailService(OrderRepository orderRepository, OrderFeePolicyPort orderFeePolicyPort) {
        this.orderRepository = orderRepository;
        this.orderFeePolicyPort = orderFeePolicyPort;
    }

    public GetMerchantOrderDetailService(OrderRepository orderRepository) {
        this(orderRepository, null);
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

        BigDecimal takeRate = orderFeePolicyPort != null ? orderFeePolicyPort.getTakeRate() : new BigDecimal("0.0350");
        BigDecimal fixedFee = orderFeePolicyPort != null ? orderFeePolicyPort.getFixedPlatformFee() : new BigDecimal("0.90");

        return MerchantOrderDetailResponse.from(order, takeRate, fixedFee);
    }
}
