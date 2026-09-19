package com.jaldishop.backend.notification.infrastructure.persistence.mapper;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceMapper {

    public Notification toDomain(NotificationEntity entity) {
        return Notification.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getReadAt());
    }

    public NotificationEntity toEntity(Notification domain) {
        return new NotificationEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getType(),
                domain.getTitle(),
                domain.getMessage(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getReadAt());
    }
}
