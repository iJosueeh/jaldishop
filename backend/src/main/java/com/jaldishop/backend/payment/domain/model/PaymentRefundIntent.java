package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado de dominio que modela la intención única y persistente de reembolso de un pago.
 * Mantiene la clave de idempotencia estable X-Idempotency-Key y el token de lease
 * para asegurar concurrencia sin carreras entre workers.
 */
public class PaymentRefundIntent {

    private final UUID id;
    private final UUID paymentId;
    private final String checkoutMode;
    private final String providerOrderId;
    private final String providerPaymentId;
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;
    private RefundIntentStatus status;
    private UUID claimToken;
    private Instant leaseExpiresAt;
    private String providerRefundId;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public static PaymentRefundIntent create(
            UUID paymentId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            UUID initialClaimToken,
            Duration initialLease,
            Instant now
    ) {
        return create(paymentId, "PREFERENCES", null, providerPaymentId, amount, currency, idempotencyKey, initialClaimToken, initialLease, now);
    }

    public static PaymentRefundIntent create(
            UUID paymentId,
            String checkoutMode,
            String providerOrderId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            UUID initialClaimToken,
            Duration initialLease,
            Instant now
    ) {
        Objects.requireNonNull(paymentId, "paymentId no puede ser nulo");
        Objects.requireNonNull(amount, "amount no puede ser nulo");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey no puede ser nulo");

        String mode = checkoutMode != null && !checkoutMode.isBlank() ? checkoutMode : "PREFERENCES";
        if ("ORDERS".equalsIgnoreCase(mode)) {
            Objects.requireNonNull(providerOrderId, "providerOrderId no puede ser nulo para reembolsos en modo ORDERS");
        } else {
            Objects.requireNonNull(providerPaymentId, "providerPaymentId no puede ser nulo para reembolsos en modo PREFERENCES");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a reembolsar debe ser mayor a cero");
        }

        Instant effectiveNow = now != null ? now : Instant.now();
        Instant leaseExpires = initialLease != null ? effectiveNow.plus(initialLease) : null;

        return new PaymentRefundIntent(
                UUID.randomUUID(),
                paymentId,
                mode,
                providerOrderId,
                providerPaymentId,
                amount,
                currency != null ? currency : "PEN",
                idempotencyKey,
                RefundIntentStatus.PROCESSING,
                initialClaimToken,
                leaseExpires,
                null,
                null,
                effectiveNow,
                effectiveNow,
                null
        );
    }

    public static PaymentRefundIntent rehydrate(
            UUID id,
            UUID paymentId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            RefundIntentStatus status,
            UUID claimToken,
            Instant leaseExpiresAt,
            String providerRefundId,
            String failureReason,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        return rehydrate(
                id, paymentId, "PREFERENCES", null, providerPaymentId, amount, currency, idempotencyKey,
                status, claimToken, leaseExpiresAt, providerRefundId, failureReason,
                createdAt, updatedAt, completedAt
        );
    }

    public static PaymentRefundIntent rehydrate(
            UUID id,
            UUID paymentId,
            String checkoutMode,
            String providerOrderId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            RefundIntentStatus status,
            UUID claimToken,
            Instant leaseExpiresAt,
            String providerRefundId,
            String failureReason,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        return new PaymentRefundIntent(
                id, paymentId, checkoutMode != null ? checkoutMode : "PREFERENCES",
                providerOrderId, providerPaymentId, amount, currency, idempotencyKey,
                status, claimToken, leaseExpiresAt, providerRefundId, failureReason,
                createdAt, updatedAt, completedAt
        );
    }

    private PaymentRefundIntent(
            UUID id,
            UUID paymentId,
            String checkoutMode,
            String providerOrderId,
            String providerPaymentId,
            BigDecimal amount,
            String currency,
            String idempotencyKey,
            RefundIntentStatus status,
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
        this.checkoutMode = checkoutMode != null ? checkoutMode : "PREFERENCES";
        this.providerOrderId = providerOrderId;
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

    public boolean canBeClaimed(Instant now) {
        if (status == RefundIntentStatus.COMPLETED || status == RefundIntentStatus.FAILED) {
            return false;
        }
        if (status == RefundIntentStatus.PENDING || status == RefundIntentStatus.UNKNOWN) {
            return true;
        }
        // Si está en PROCESSING, solo puede ser reclamado si su lease ha expirado
        return leaseExpiresAt == null || leaseExpiresAt.isBefore(now);
    }

    public void acquireClaim(UUID workerToken, Duration leaseDuration, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.claimToken = Objects.requireNonNull(workerToken, "workerToken no puede ser nulo");
        this.leaseExpiresAt = effectiveNow.plus(Objects.requireNonNull(leaseDuration, "leaseDuration no puede ser nulo"));
        this.status = RefundIntentStatus.PROCESSING;
        this.updatedAt = effectiveNow;
    }

    public boolean isClaimOwnedBy(UUID workerToken, Instant now) {
        if (workerToken == null || this.claimToken == null) {
            return false;
        }
        return this.claimToken.equals(workerToken);
    }

    public void complete(String providerRefundId, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.status = RefundIntentStatus.COMPLETED;
        this.providerRefundId = Objects.requireNonNull(providerRefundId, "providerRefundId no puede ser nulo");
        this.failureReason = null;
        this.completedAt = effectiveNow;
        this.updatedAt = effectiveNow;
    }

    public void fail(String reason, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.status = RefundIntentStatus.FAILED;
        this.failureReason = reason != null ? reason : "Rechazo definitivo del proveedor";
        this.completedAt = effectiveNow;
        this.updatedAt = effectiveNow;
    }

    public void markUnknown(String reason, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.status = RefundIntentStatus.UNKNOWN;
        this.failureReason = reason != null ? reason : "Resultado de la llamada externa indeterminado";
        this.updatedAt = effectiveNow;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getCheckoutMode() { return checkoutMode; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public RefundIntentStatus getStatus() { return status; }
    public UUID getClaimToken() { return claimToken; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public String getProviderRefundId() { return providerRefundId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
