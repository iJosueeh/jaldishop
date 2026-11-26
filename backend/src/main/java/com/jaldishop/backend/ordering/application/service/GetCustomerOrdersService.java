package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.model.OrderItem;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderSummaryResponse;
import com.jaldishop.backend.shared.web.dto.CustomerPageResponse;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        return mapOrders(orders);
    }

    @Transactional(readOnly = true)
    public CustomerPageResponse<CustomerOrderSummaryResponse> executePaged(
            UUID customerUserId,
            String tab,
            String deliveryModeParam,
            String q,
            int page,
            int size
    ) {
        if (customerUserId == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        boolean activeOnly = tab == null || !"HISTORY".equalsIgnoreCase(tab.trim());
        DeliveryMode deliveryMode = parseDeliveryMode(deliveryModeParam);
        String searchPattern = buildSearchPattern(q);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Order> resultPage = orderRepository.findPagedByUserId(
                customerUserId,
                activeOnly,
                deliveryMode,
                searchPattern,
                pageable
        );

        return new CustomerPageResponse<>(
                mapOrders(resultPage.getContent()),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private List<CustomerOrderSummaryResponse> mapOrders(List<Order> orders) {
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

    private DeliveryMode parseDeliveryMode(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return DeliveryMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
