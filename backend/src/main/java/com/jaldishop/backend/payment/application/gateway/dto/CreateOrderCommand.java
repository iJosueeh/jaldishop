package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Comando de dominio que solicita la creación de una orden en Checkout Pro mediante Orders API (/v1/orders).
 * Desacoplado de especificaciones JSON del proveedor.
 */
public record CreateOrderCommand(
        String externalReference,
        BigDecimal totalAmount,
        List<OrderItem> items,
        PayerInfo payer,
        String notificationUrl,
        String successBackUrl,
        String failureBackUrl,
        String pendingBackUrl,
        String statementDescriptor,
        String idempotencyKey,
        String expirationTime
) {

    public CreateOrderCommand(
            String externalReference,
            BigDecimal totalAmount,
            List<OrderItem> items,
            PayerInfo payer,
            String notificationUrl,
            String successBackUrl,
            String failureBackUrl,
            String pendingBackUrl,
            String statementDescriptor,
            String idempotencyKey
    ) {
        this(externalReference, totalAmount, items, payer, notificationUrl, successBackUrl,
                failureBackUrl, pendingBackUrl, statementDescriptor, idempotencyKey, null);
    }

    public CreateOrderCommand {
        Objects.requireNonNull(externalReference, "externalReference no puede ser nulo");
        if (externalReference.isBlank()) {
            throw new IllegalArgumentException("externalReference no puede estar en blanco");
        }
        Objects.requireNonNull(totalAmount, "totalAmount no puede ser nulo");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount debe ser mayor a cero");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un ítem en la orden");
        }
    }

    public record OrderItem(
            String id,
            String title,
            String description,
            int quantity,
            BigDecimal unitPrice,
            String currency
    ) {
        public OrderItem {
            Objects.requireNonNull(title, "title no puede ser nulo");
            Objects.requireNonNull(unitPrice, "unitPrice no puede ser nulo");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity debe ser mayor a cero");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("unitPrice debe ser mayor a cero");
            }
        }
    }

    public record PayerInfo(
            String name,
            String surname,
            String email,
            String phone
    ) {}

}
