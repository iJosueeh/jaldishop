package com.jaldishop.backend.notification.domain.repository;

import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Optional<Notification> findById(UUID id);
    List<Notification> findByUserId(UUID userId, Pageable pageable);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
    Notification save(Notification notification);
}
