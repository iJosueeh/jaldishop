package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.model.PaymentRefundExecutionAttempt;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentRefundExecutionAttemptEntity;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentRefundIntentEntity;

public final class PaymentRefundPersistenceMapper {

    private PaymentRefundPersistenceMapper() {
    }

    public static PaymentRefundIntent toDomain(PaymentRefundIntentEntity entity) {
        if (entity == null) return null;
        return PaymentRefundIntent.rehydrate(
                entity.getId(),
                entity.getPaymentId(),
                entity.getProviderPaymentId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getIdempotencyKey(),
                RefundIntentStatus.valueOf(entity.getStatus()),
                entity.getClaimToken(),
                entity.getLeaseExpiresAt(),
                entity.getProviderRefundId(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt()
        );
    }

    public static PaymentRefundIntentEntity toEntity(PaymentRefundIntent domain) {
        if (domain == null) return null;
        return new PaymentRefundIntentEntity(
                domain.getId(),
                domain.getPaymentId(),
                domain.getProviderPaymentId(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getIdempotencyKey(),
                domain.getStatus().name(),
                domain.getClaimToken(),
                domain.getLeaseExpiresAt(),
                domain.getProviderRefundId(),
                domain.getFailureReason(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getCompletedAt()
        );
    }

    public static PaymentRefundExecutionAttempt toDomain(PaymentRefundExecutionAttemptEntity entity) {
        if (entity == null) return null;
        return new PaymentRefundExecutionAttempt(
                entity.getId(),
                entity.getRefundIntentId(),
                entity.getAttemptNumber(),
                entity.getRequestMethod(),
                entity.getStatus(),
                entity.getHttpStatus(),
                entity.getResponseBody(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getExecutedAt()
        );
    }

    public static PaymentRefundExecutionAttemptEntity toEntity(PaymentRefundExecutionAttempt domain) {
        if (domain == null) return null;
        return new PaymentRefundExecutionAttemptEntity(
                domain.getId(),
                domain.getRefundIntentId(),
                domain.getAttemptNumber(),
                domain.getRequestMethod(),
                domain.getStatus(),
                domain.getHttpStatus(),
                domain.getResponseBody(),
                domain.getErrorCode(),
                domain.getErrorMessage(),
                domain.getExecutedAt()
        );
    }
}
