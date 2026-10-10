package com.jaldishop.backend.checkout.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo de SOLO LECTURA de la tabla payments para ConfirmPurchase.
 * El módulo de pagos (BE-PAYMENTS-01) formalizará su propio dominio.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "capacity_reservation_id", nullable = false)
    private UUID capacityReservationId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected PaymentEntity() {
    }

    public UUID getId() { return id; }
    public UUID getCapacityReservationId() { return capacityReservationId; }
    public String getStatus() { return status; }
    public Instant getApprovedAt() { return approvedAt; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

}