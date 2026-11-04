package com.jaldishop.backend.payment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que audita cada interacción HTTP técnica (POST o GET) realizada
 * contra Mercado Pago para ejecutar o reconciliar una intención de reembolso.
 */
public class PaymentRefundExecutionAttempt {

    private final UUID id;
    private final UUID refundIntentId;
    private final int attemptNumber;
    private final String requestMethod;
    private final String status; // SUCCESSFUL, FAILED, UNKNOWN
    private final Integer httpStatus;
    private final String responseBody;
    private final String errorCode;
    private final String errorMessage;
    private final Instant executedAt;

    public static PaymentRefundExecutionAttempt record(
            UUID refundIntentId,
            int attemptNumber,
            String requestMethod,
            String status,
            Integer httpStatus,
            String responseBody,
            String errorCode,
            String errorMessage,
            Instant executedAt
    ) {
        return new PaymentRefundExecutionAttempt(
                UUID.randomUUID(),
                Objects.requireNonNull(refundIntentId, "refundIntentId no puede ser nulo"),
                attemptNumber,
                Objects.requireNonNull(requestMethod, "requestMethod no puede ser nulo"),
                Objects.requireNonNull(status, "status no puede ser nulo"),
                httpStatus,
                responseBody,
                errorCode,
                errorMessage,
                executedAt != null ? executedAt : Instant.now()
        );
    }

    public PaymentRefundExecutionAttempt(
            UUID id,
            UUID refundIntentId,
            int attemptNumber,
            String requestMethod,
            String status,
            Integer httpStatus,
            String responseBody,
            String errorCode,
            String errorMessage,
            Instant executedAt
    ) {
        this.id = Objects.requireNonNull(id, "id no puede ser nulo");
        this.refundIntentId = Objects.requireNonNull(refundIntentId, "refundIntentId no puede ser nulo");
        this.attemptNumber = attemptNumber;
        this.requestMethod = Objects.requireNonNull(requestMethod, "requestMethod no puede ser nulo");
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.executedAt = executedAt != null ? executedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRefundIntentId() { return refundIntentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getRequestMethod() { return requestMethod; }
    public String getStatus() { return status; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getExecutedAt() { return executedAt; }
}
