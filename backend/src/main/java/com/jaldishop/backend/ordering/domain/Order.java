package com.jaldishop.backend.ordering.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;

public class Order {

    private Long id;
    private OrderStatus status;
    private final DeliveryMode deliveryMode;

    public Order(Long id, DeliveryMode deliveryMode) {
        this.id = id;
        this.deliveryMode = deliveryMode;
        this.status = OrderStatus.CONFIRMADO;
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessRuleException(
                    "RN-ORD-07",
                    String.format("Transición inválida de estado: no se puede pasar de %s a %s", this.status, newStatus)
            );
        }

        if (newStatus == OrderStatus.EN_ENTREGA && this.deliveryMode != DeliveryMode.DELIVERY) {
            throw new BusinessRuleException(
                    "RN-DEL-04",
                    "El estado EN_ENTREGA solo está permitido para pedidos con modalidad DELIVERY"
            );
        }

        this.status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

}
