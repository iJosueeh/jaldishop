package com.jaldishop.backend.notification.application.service;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import com.jaldishop.backend.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IsolatedOrderNotificationPersister {

    private final CreateNotificationService createNotificationService;

    public IsolatedOrderNotificationPersister(CreateNotificationService createNotificationService) {
        this.createNotificationService = createNotificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification persistIsolated(CreateNotificationCommand command) {
        return createNotificationService.execute(command);
    }

}
