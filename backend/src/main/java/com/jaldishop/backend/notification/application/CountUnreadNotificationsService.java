package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.NotificationRepository;
import com.jaldishop.backend.notification.domain.NotificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CountUnreadNotificationsService {

    private final NotificationRepository notificationRepository;

    public CountUnreadNotificationsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public long execute(UUID userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }
}
