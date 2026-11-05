package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentMethod;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que audita cada intento de pago contra un proveedor externo (Mercado Pago).
 * Registra claves de idempotencia, identificadores de preferencia, IDs del proveedor y detalles de estado.
 */
public class PaymentAttempt {

    private final UUID id;
    private final UUID paymentId;
    private final int attemptNumber;
    private final PaymentProvider provider;
    private PaymentMethod paymentMethod;
    private final String idempotencyKey;
    private String checkoutMode;
    private String preferenceId;
    private String providerOrderId;
    private String initPoint;
    private String providerPaymentId;
    private String providerStatus;
    private String providerStatusDetail;
    private PaymentAttemptStatus status;
    private String errorCode;
    private String errorMessage;
    private final Instant createdAt;
    private Instant completedAt;
    private Instant updatedAt;

    public static PaymentAttempt create(
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey,
            String checkoutMode
    ) {
        Objects.requireNonNull(paymentId, "paymentId no puede ser nulo");
        Objects.requireNonNull(provider, "provider no puede ser nulo");
        Objects.requireNonNull(paymentMethod, "paymentMethod no puede ser nulo");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey no puede ser nulo");
        if (attemptNumber <= 0) {
            throw new IllegalArgumentException("attemptNumber debe ser mayor a cero");
        }

        Instant now = Instant.now();
        return new PaymentAttempt(
                UUID.randomUUID(),
                paymentId,
                attemptNumber,
                provider,
                paymentMethod,
                idempotencyKey,
                checkoutMode != null ? checkoutMode : "PREFERENCES",
                null,
                null,
                null,
                null,
                null,
                null,
                PaymentAttemptStatus.CREATED,
                null,
                null,
                now,
                null,
                now
        );
    }

    public static PaymentAttempt create(
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey
    ) {
        return create(paymentId, attemptNumber, provider, paymentMethod, idempotencyKey, "PREFERENCES");
    }

    public static PaymentAttempt rehydrate(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey,
            String preferenceId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentAttemptStatus status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        return rehydrate(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                "PREFERENCES", preferenceId, null, initPoint, providerPaymentId, providerStatus, providerStatusDetail,
                status, errorCode, errorMessage, createdAt, completedAt, updatedAt
        );
    }

    public static PaymentAttempt rehydrate(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentAttemptStatus status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        return rehydrate(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                providerOrderId != null ? "ORDERS" : "PREFERENCES",
                preferenceId, providerOrderId, initPoint, providerPaymentId, providerStatus, providerStatusDetail,
                status, errorCode, errorMessage, createdAt, completedAt, updatedAt
        );
    }

    public static PaymentAttempt rehydrate(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey,
            String checkoutMode,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentAttemptStatus status,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            Instant updatedAt
    ) {
        return new PaymentAttempt(
                id, paymentId, attemptNumber, provider, paymentMethod, idempotencyKey,
                checkoutMode != null ? checkoutMode : "PREFERENCES",
                preferenceId, providerOrderId, initPoint, providerPaymentId, providerStatus, providerStatusDetail,
                status, errorCode, errorMessage, createdAt, completedAt, updatedAt
        );
    }

    private PaymentAttempt(
            UUID id,
            UUID paymentId,
            int attemptNumber,
            PaymentProvider provider,
            PaymentMethod paymentMethod,
            String idempotencyKey,
            String checkoutMode,
            String preferenceId,
            String providerOrderId,
            String initPoint,
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentAttemptStatus status,
            String errorCode,
            String errorMessage,
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
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public void recordPreference(String preferenceId, String initPoint) {
        this.preferenceId = preferenceId;
        this.initPoint = initPoint;
        this.updatedAt = Instant.now();
    }

    public void recordOrder(String providerOrderId, String initPoint) {
        Objects.requireNonNull(providerOrderId, "providerOrderId no puede ser nulo");
        if (providerOrderId.isBlank()) {
            throw new IllegalArgumentException("providerOrderId no puede estar en blanco");
        }
        this.providerOrderId = providerOrderId;
        this.initPoint = initPoint;
        this.updatedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = PaymentAttemptStatus.PROCESSING;
        this.errorCode = null;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markApproved(
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentMethod resolvedMethod,
            Instant completedAt
    ) {
        this.status = PaymentAttemptStatus.APPROVED;
        this.providerPaymentId = providerPaymentId;
        this.providerStatus = providerStatus;
        this.providerStatusDetail = providerStatusDetail;
        if (resolvedMethod != null) {
            this.paymentMethod = resolvedMethod;
        }
        this.completedAt = completedAt != null ? completedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markProviderPending(
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            PaymentMethod resolvedMethod
    ) {
        this.status = PaymentAttemptStatus.PROCESSING;
        this.providerPaymentId = providerPaymentId;
        this.providerStatus = providerStatus;
        this.providerStatusDetail = providerStatusDetail;
        if (resolvedMethod != null) {
            this.paymentMethod = resolvedMethod;
        }
        this.updatedAt = Instant.now();
    }

    public void markRejected(
            String providerPaymentId,
            String providerStatus,
            String providerStatusDetail,
            String errorCode,
            String errorMessage,
            Instant completedAt
    ) {
        this.status = PaymentAttemptStatus.REJECTED;
        this.providerPaymentId = providerPaymentId;
        this.providerStatus = providerStatus;
        this.providerStatusDetail = providerStatusDetail;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = completedAt != null ? completedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markError(String errorCode, String errorMessage) {
        this.status = PaymentAttemptStatus.ERROR;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordIncidence(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCheckoutMode() { return checkoutMode; }
    public String getPreferenceId() { return preferenceId; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getInitPoint() { return initPoint; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public String getProviderStatus() { return providerStatus; }
    public String getProviderStatusDetail() { return providerStatusDetail; }
    public PaymentAttemptStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

}
