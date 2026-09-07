package com.jaldishop.backend.ordering.domain;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    boolean existsById(Long id);
}
