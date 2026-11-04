package com.jaldishop.backend.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refund_intents")
public class PaymentRefundIntentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "provider_payment_id", nullable = false, length = 100)
    private String providerPaymentId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public PaymentRefundIntentEntity() {
    }

    public PaymentRefundIntentEntity(
            UUID id,
            UUID paymentId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            String status,
            UUID claimToken,
            Instant leaseExpiresAt,
            String providerRefundId,
            String failureReason,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.providerPaymentId = providerPaymentId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.claimToken = claimToken;
        this.leaseExpiresAt = leaseExpiresAt;
        this.providerRefundId = providerRefundId;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getClaimToken() { return claimToken; }
    public void setClaimToken(UUID claimToken) { this.claimToken = claimToken; }

    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }

    public String getProviderRefundId() { return providerRefundId; }
    public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
