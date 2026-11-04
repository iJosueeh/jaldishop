package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentIncidenceEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentIncidencePersistenceMapper {

    public PaymentIncidence toDomain(PaymentIncidenceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentIncidence(
                entity.getId(),
                entity.getPaymentId(),
                IncidenceProcessType.valueOf(entity.getProcessType()),
                IncidenceCategory.valueOf(entity.getCategory()),
                entity.getCode(),
                entity.getMessage(),
                entity.getAttemptsCount(),
                entity.getMaxAttempts(),
                entity.getNextRetryAt(),
                IncidenceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getResolvedAt()
        );
    }

    public PaymentIncidenceEntity toEntity(PaymentIncidence domain) {
        if (domain == null) {
            return null;
        }
        return new PaymentIncidenceEntity(
                domain.getId(),
                domain.getPaymentId(),
                domain.getProcessType().name(),
                domain.getCategory().name(),
                domain.getCode(),
                domain.getMessage(),
                domain.getAttemptsCount(),
                domain.getMaxAttempts(),
                domain.getNextRetryAt(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getResolvedAt()
        );
    }
}
