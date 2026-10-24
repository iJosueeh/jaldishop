package com.jaldishop.backend.store.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de lectura puro de dominio para la lista de clientes de una tienda.
 * Mismos 9 campos, tipos y orden que el contrato REST {@code StoreCustomerResponse}
 * (que vive exclusivamente en la capa web y se construye allí a partir de este resumen).
 */
public record StoreCustomerSummary(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant customerSince,
        long ordersCount,
        BigDecimal totalSpentAmount,
        Instant lastOrderAt
) {}
