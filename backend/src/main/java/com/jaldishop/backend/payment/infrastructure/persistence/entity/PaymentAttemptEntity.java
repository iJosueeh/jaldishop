package com.jaldishop.backend.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "checkout_mode", nullable = false, length = 20)
    private String checkoutMode;

    @Column(name = "preference_id", length = 100)
    private String preferenceId;

    @Column(name = "provider_order_id", length = 100)
    private String providerOrderId;

    @Column(name = "init_point")
    private String initPoint;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "provider_status", length = 50)
    private String providerStatus;

    @Column(name = "provider_status_detail", length = 100)
    private String providerStatusDetail;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "reconciliation_claim_token")
    private UUID reconciliationClaimToken;

    @Column(name = "reconciliation_lease_expires_at")
    private Instant reconciliationLeaseExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentAttemptEntity() {
    }

    public PaymentAttemptEntity(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            String provider,
            String paymentMethod,
            String idempotencyKey,
            String preferenceId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            String status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        this(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                preferenceId, null, initPoint, providerPaymentId, providerStatus, providerStatusDetail,
                status, errorCode, errorMessage, createdAt, completedAt, updatedAt
        );
    }

    public PaymentAttemptEntity(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            String provider,
            String paymentMethod,
            String idempotencyKey,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            String status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        this(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                "PREFERENCES", preferenceId, providerOrderId, initPoint, providerPaymentId,
                providerStatus, providerStatusDetail, status, errorCode, errorMessage,
                createdAt, completedAt, updatedAt
        );
    }

    public PaymentAttemptEntity(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            String provider,
            String paymentMethod,
            String idempotencyKey,
            String checkoutMode,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            String status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        this(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                checkoutMode, preferenceId, providerOrderId, initPoint, providerPaymentId,
                providerStatus, providerStatusDetail, status, errorCode, errorMessage,
                null, null, createdAt, completedAt, updatedAt
        );
    }

    public PaymentAttemptEntity(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            String provider,
            String paymentMethod,
            String idempotencyKey,
            String checkoutMode,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            String status,
            String errorCode,
            String errorMessage,
            UUID reconciliationClaimToken,
            Instant reconciliationLeaseExpiresAt,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.attemptNumber = attemptNumber;
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.checkoutMode = checkoutMode != null ? checkoutMode : "PREFERENCES";
        this.preferenceId = preferenceId;
        this.providerOrderId = providerOrderId;
        this.initPoint = initPoint;
        this.providerPaymentId = providerPaymentId;
        this.providerStatus = providerStatus;
        this.providerStatusDetail = providerStatusDetail;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.reconciliationClaimToken = reconciliationClaimToken;
        this.reconciliationLeaseExpiresAt = reconciliationLeaseExpiresAt;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCheckoutMode() { return checkoutMode; }
    public void setCheckoutMode(String checkoutMode) { this.checkoutMode = checkoutMode; }
    public String getPreferenceId() { return preferenceId; }
    public void setPreferenceId(String preferenceId) { this.preferenceId = preferenceId; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }
    public String getInitPoint() { return initPoint; }
    public void setInitPoint(String initPoint) { this.initPoint = initPoint; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
    public String getProviderStatus() { return providerStatus; }
    public void setProviderStatus(String providerStatus) { this.providerStatus = providerStatus; }
    public String getProviderStatusDetail() { return providerStatusDetail; }
    public void setProviderStatusDetail(String providerStatusDetail) { this.providerStatusDetail = providerStatusDetail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getReconciliationClaimToken() { return reconciliationClaimToken; }
    public void setReconciliationClaimToken(UUID reconciliationClaimToken) { this.reconciliationClaimToken = reconciliationClaimToken; }
    public Instant getReconciliationLeaseExpiresAt() { return reconciliationLeaseExpiresAt; }
    public void setReconciliationLeaseExpiresAt(Instant reconciliationLeaseExpiresAt) { this.reconciliationLeaseExpiresAt = reconciliationLeaseExpiresAt; }

}
