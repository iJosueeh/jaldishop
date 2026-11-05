package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
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

        return PaymentAttempt.rehydrate(
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
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentAttemptEntity toEntity(PaymentAttempt domain) {
        if (domain == null) {
            return null;
        }

        return new PaymentAttemptEntity(
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
                domain.getCreatedAt(),
                domain.getCompletedAt(),
                domain.getUpdatedAt()
        );
    }

}
