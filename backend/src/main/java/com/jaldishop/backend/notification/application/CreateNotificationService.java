package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateNotificationService {

    private final NotificationRepository notificationRepository;

    public CreateNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification execute(CreateNotificationCommand command) {
        Notification notification = Notification.create(
                command.userId(),
                command.type(),
                command.title(),
                command.message());
        return notificationRepository.save(notification);
    }
}
