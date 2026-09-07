package com.jaldishop.backend.ordering.application;

import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.ordering.domain.OrderStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeOrderStatusService {

    private final OrderRepository orderRepository;

    public ChangeOrderStatusService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void execute(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        order.transitionTo(newStatus);

        orderRepository.save(order);
    }

}
