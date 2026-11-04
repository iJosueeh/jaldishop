package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CancelCustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderCancellationCompensator cancellationCompensator;
    private final GetCustomerOrderDetailService getOrderDetailService;
    private final ApplicationEventPublisher eventPublisher;

    public CancelCustomerOrderService(
            OrderRepository orderRepository,
            OrderCancellationCompensator cancellationCompensator,
            GetCustomerOrderDetailService getOrderDetailService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.cancellationCompensator = cancellationCompensator;
        this.getOrderDetailService = getOrderDetailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CustomerOrderDetailResponse execute(UUID customerUserId, String orderNumber, String reason) {
        if (customerUserId == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio.");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("El número de pedido es obligatorio.");
        }

        Order existingOrder = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber.trim()));

        if (!existingOrder.getUserId().equals(customerUserId)) {
            throw new AccessDeniedException("No tiene permisos para cancelar un pedido ajeno.");
        }

        Order order = orderRepository.findByIdForUpdate(existingOrder.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber.trim()));

        if (!order.getUserId().equals(customerUserId)) {
            throw new AccessDeniedException("No tiene permisos para cancelar un pedido ajeno.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return getOrderDetailService.execute(customerUserId, orderNumber.trim());
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "RN-ORD-09",
                    "El pedido no se puede cancelar porque se encuentra en estado " + order.getStatus() +
                    ". Solo los pedidos en estado CONFIRMED pueden ser cancelados por el cliente."
            );
        }

        OrderStatus previousStatus = order.getStatus();
        String cancellationReason = (reason != null && !reason.isBlank()) ? reason : "Cancelación solicitada por el cliente.";
        order.transitionTo(OrderStatus.CANCELLED, customerUserId, cancellationReason);

        Order savedOrder = orderRepository.save(order);

        if (cancellationCompensator != null) {
            cancellationCompensator.compensate(savedOrder, previousStatus, cancellationReason);
        }

        eventPublisher.publishEvent(new OrderStatusChangedDomainEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getUserId(),
                savedOrder.getStoreId(),
                previousStatus,
                savedOrder.getStatus()
        ));

        return getOrderDetailService.execute(customerUserId, orderNumber.trim());
    }
}
