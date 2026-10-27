package com.jaldishop.backend.ordering.application;

import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderItem;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderSummaryResponse;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GetCustomerOrdersService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public GetCustomerOrdersService(OrderRepository orderRepository, StoreRepository storeRepository) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> execute(UUID customerUserId) {
        if (customerUserId == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio.");
        }

        List<Order> orders = orderRepository.findByUserId(customerUserId);
        Map<UUID, Store> storeCache = new ConcurrentHashMap<>();

        return orders.stream()
                .map(order -> {
                    Store store = storeCache.computeIfAbsent(
                            order.getStoreId(),
                            id -> storeRepository.findById(id).orElse(null)
                    );

                    String storeName = store != null ? store.getName() : "Comercio Local";
                    String storeSlug = store != null ? store.getSlug() : "";
                    String storeLogoUrl = store != null ? store.getLogoUrl() : null;

                    int totalItems = order.getItems().stream()
                            .mapToInt(OrderItem::quantity)
                            .sum();

                    return new CustomerOrderSummaryResponse(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getStoreId(),
                            storeName,
                            storeSlug,
                            storeLogoUrl,
                            order.getStatus(),
                            order.getDeliveryMode(),
                            order.getServiceDate(),
                            order.getServiceStartTime(),
                            order.getServiceEndTime(),
                            order.getTotalAmount(),
                            order.getCurrency(),
                            totalItems,
                            order.getConfirmedAt()
                    );
                })
                .toList();
    }

}
