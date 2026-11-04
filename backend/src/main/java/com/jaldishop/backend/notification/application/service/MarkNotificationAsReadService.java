package com.jaldishop.backend.notification.application.service;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import com.jaldishop.backend.notification.domain.model.Notification;
import com.jaldishop.backend.notification.domain.repository.NotificationRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MarkNotificationAsReadService {

    private final NotificationRepository notificationRepository;

    public MarkNotificationAsReadService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification execute(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }

        notification.markAsRead();
        return notificationRepository.save(notification);
    }
}
