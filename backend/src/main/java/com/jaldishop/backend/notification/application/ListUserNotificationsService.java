package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListUserNotificationsService {

    private final NotificationRepository notificationRepository;

    public ListUserNotificationsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> execute(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }
}
