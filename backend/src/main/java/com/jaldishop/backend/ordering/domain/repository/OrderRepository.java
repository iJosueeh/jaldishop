package com.jaldishop.backend.ordering.domain.repository;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByIdForUpdate(UUID id);
    Optional<Order> findByPaymentId(UUID paymentId);
    Optional<Order> findByCapacityReservationId(UUID capacityReservationId);
    List<Order> findByUserId(UUID userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long nextOrderNumber();
    boolean hasCompletedOrderWithProduct(UUID userId, UUID productId);

    Page<Order> findByStoreId(UUID storeId, Pageable pageable);
    Page<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status, Pageable pageable);
    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);
    Optional<Order> findByOrderNumberAndStoreId(String orderNumber, UUID storeId);
}
