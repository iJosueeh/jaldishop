package com.jaldishop.backend.ordering.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByPaymentId(UUID paymentId);
    Optional<Order> findByCapacityReservationId(UUID capacityReservationId);
    long nextOrderNumber();
}