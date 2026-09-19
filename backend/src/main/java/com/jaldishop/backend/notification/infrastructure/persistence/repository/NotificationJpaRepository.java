package com.jaldishop.backend.notification.infrastructure.persistence.repository;

import com.jaldishop.backend.notification.domain.NotificationStatus;
import com.jaldishop.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
}
