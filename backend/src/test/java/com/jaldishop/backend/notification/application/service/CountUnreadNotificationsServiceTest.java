package com.jaldishop.backend.notification.application.service;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.application.service.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import com.jaldishop.backend.notification.domain.repository.NotificationRepository;
import com.jaldishop.backend.notification.domain.enums.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountUnreadNotificationsServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private CountUnreadNotificationsService countUnreadNotificationsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        countUnreadNotificationsService = new CountUnreadNotificationsService(notificationRepository);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Contar notificaciones no leídas")
    void countUnreadNotifications() {
        when(notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD)).thenReturn(5L);

        long count = countUnreadNotificationsService.execute(userId);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Retornar cero cuando no hay notificaciones no leídas")
    void returnZeroWhenNoUnread() {
        when(notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD)).thenReturn(0L);

        long count = countUnreadNotificationsService.execute(userId);

        assertEquals(0L, count);
    }
}
