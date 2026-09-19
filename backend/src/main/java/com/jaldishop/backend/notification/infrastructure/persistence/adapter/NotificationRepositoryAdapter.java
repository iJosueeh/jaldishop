package com.jaldishop.backend.notification.infrastructure.persistence.adapter;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationRepository;
import com.jaldishop.backend.notification.domain.NotificationStatus;
import com.jaldishop.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.jaldishop.backend.notification.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import com.jaldishop.backend.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationPersistenceMapper mapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository, NotificationPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, NotificationStatus status) {
        return jpaRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = mapper.toEntity(notification);
        NotificationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
