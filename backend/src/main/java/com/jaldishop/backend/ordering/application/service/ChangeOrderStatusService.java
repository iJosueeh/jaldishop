package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ChangeOrderStatusService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChangeOrderStatusService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID orderId, OrderStatus newStatus, UUID expectedStoreId) {
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(newStatus, "newStatus no puede ser nulo");
        if (expectedStoreId == null) {
            throw new AccessDeniedException("El contexto de tienda (expectedStoreId) es obligatorio para autorizar la operación.");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        if (!order.getStoreId().equals(expectedStoreId)) {
            throw new AccessDeniedException("No tiene permisos para modificar un pedido de otra tienda.");
        }

        if (order.getStatus() == newStatus) {
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.transitionTo(newStatus, null, null);

        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderStatusChangedDomainEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getUserId(),
                savedOrder.getStoreId(),
                previousStatus,
                savedOrder.getStatus()
        ));
    }

}
