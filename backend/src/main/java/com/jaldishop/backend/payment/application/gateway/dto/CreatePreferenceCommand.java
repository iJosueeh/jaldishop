package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Comando de dominio que solicita la creación de una preferencia de Checkout Pro.
 * No depende de estructuras JSON externas.
 */
public record CreatePreferenceCommand(
        String externalReference,
        String title,
        List<PreferenceItem> items,
        PayerInfo payer,
        String notificationUrl,
        String successBackUrl,
        String failureBackUrl,
        String pendingBackUrl,
        String statementDescriptor,
        String idempotencyKey
) {

    public CreatePreferenceCommand(
            String externalReference,
            String title,
            List<PreferenceItem> items,
            PayerInfo payer,
            String notificationUrl,
            String successBackUrl,
            String failureBackUrl,
            String pendingBackUrl,
            String statementDescriptor
    ) {
        this(externalReference, title, items, payer, notificationUrl, successBackUrl, failureBackUrl, pendingBackUrl, statementDescriptor, null);
    }

    public CreatePreferenceCommand {
        Objects.requireNonNull(externalReference, "externalReference no puede ser nulo");
        if (externalReference.isBlank()) {
            throw new IllegalArgumentException("externalReference no puede estar en blanco");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un ítem en la preferencia");
        }
    }

    public record PreferenceItem(
            String id,
            String title,
            String description,
            int quantity,
            BigDecimal unitPrice,
            String currency
    ) {
        public PreferenceItem {
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
