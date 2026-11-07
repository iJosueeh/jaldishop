package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.enums.RemoteOrderCreationState;
import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentMethod;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentAttemptEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentAttemptPersistenceMapper {

    public PaymentAttempt toDomain(PaymentAttemptEntity entity) {
        if (entity == null) {
            return null;
        }

        PaymentAttempt attempt = PaymentAttempt.rehydrate(
                entity.getId(),
                entity.getPaymentId(),
                entity.getAttemptNumber(),
                PaymentProvider.valueOf(entity.getProvider()),
                PaymentMethod.valueOf(entity.getPaymentMethod()),
                entity.getIdempotencyKey(),
                entity.getCheckoutMode(),
                entity.getPreferenceId(),
                entity.getProviderOrderId(),
                entity.getInitPoint(),
                entity.getProviderPaymentId(),
                entity.getProviderStatus(),
                entity.getProviderStatusDetail(),
                PaymentAttemptStatus.valueOf(entity.getStatus()),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getReconciliationClaimToken(),
                entity.getReconciliationLeaseExpiresAt(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getUpdatedAt()
        );
        attempt.restoreRemoteOrderCreationState(entity.getRemoteOrderCreationState() == null ? null
                : RemoteOrderCreationState.valueOf(entity.getRemoteOrderCreationState()));
        return attempt;
    }

    public PaymentAttemptEntity toEntity(PaymentAttempt domain) {
        if (domain == null) {
            return null;
        }

        PaymentAttemptEntity entity = new PaymentAttemptEntity(
                domain.getId(),
                domain.getPaymentId(),
                domain.getAttemptNumber(),
                domain.getProvider().name(),
                domain.getPaymentMethod().name(),
                domain.getIdempotencyKey(),
                domain.getCheckoutMode(),
                domain.getPreferenceId(),
                domain.getProviderOrderId(),
                domain.getInitPoint(),
                domain.getProviderPaymentId(),
                domain.getProviderStatus(),
                domain.getProviderStatusDetail(),
                domain.getStatus().name(),
                domain.getErrorCode(),
                domain.getErrorMessage(),
                domain.getReconciliationClaimToken(),
                domain.getReconciliationLeaseExpiresAt(),
                domain.getCreatedAt(),
                domain.getCompletedAt(),
                domain.getUpdatedAt()
        );
        entity.setRemoteOrderCreationState(domain.getRemoteOrderCreationState() == null ? null
                : domain.getRemoteOrderCreationState().name());
        return entity;
    }

}
