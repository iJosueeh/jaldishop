package com.jaldishop.backend.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refund_execution_attempts")
public class PaymentRefundExecutionAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "refund_intent_id", nullable = false)
    private UUID refundIntentId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    public PaymentRefundExecutionAttemptEntity() {
    }

    public PaymentRefundExecutionAttemptEntity(
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
        this.id = id;
        this.refundIntentId = refundIntentId;
        this.attemptNumber = attemptNumber;
        this.requestMethod = requestMethod;
        this.status = status;
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.executedAt = executedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRefundIntentId() { return refundIntentId; }
    public void setRefundIntentId(UUID refundIntentId) { this.refundIntentId = refundIntentId; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
