package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.application.result.MerchantOrderPageResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderSummaryResponse;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListMerchantOrdersService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderRepository orderRepository;

    public ListMerchantOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public MerchantOrderPageResponse execute(UUID storeId, OrderStatus status, Pageable pageable) {
        if (storeId == null) {
            throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");
        }

        Pageable effectivePageable = (pageable != null)
                ? pageable
                : PageRequest.of(0, DEFAULT_PAGE_SIZE);

        Page<Order> orderPage = (status != null)
                ? orderRepository.findByStoreIdAndStatus(storeId, status, effectivePageable)
                : orderRepository.findByStoreId(storeId, effectivePageable);

        Page<MerchantOrderSummaryResponse> summaryPage = orderPage.map(MerchantOrderSummaryResponse::from);

        return MerchantOrderPageResponse.from(summaryPage);
    }
}
