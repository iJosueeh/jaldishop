package com.jaldishop.backend.ordering.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByPaymentId(UUID paymentId);
    Optional<Order> findByCapacityReservationId(UUID capacityReservationId);
    List<Order> findByUserId(UUID userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long nextOrderNumber();
    boolean hasCompletedOrderWithProduct(UUID userId, UUID productId);
}
