package com.jaldishop.backend.realtime.application.service;

import com.jaldishop.backend.realtime.domain.event.AdminNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class AdminRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(AdminRealtimePublisher.class);

    private static final String TOPIC_ADMIN_EVENTS = "/topic/admin/events";
    private static final String USER_QUEUE_ADMIN = "/queue/admin";

    private final SimpMessagingTemplate messagingTemplate;

    public AdminRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOperationalEvent(AdminNotificationEvent event) {
        Objects.requireNonNull(event, "event no puede ser nulo");
        log.debug("Publicando evento operativo a {} (tipo: {}, recurso: {})",
                TOPIC_ADMIN_EVENTS, event.eventType(), event.resourceId());
        messagingTemplate.convertAndSend(TOPIC_ADMIN_EVENTS, event);
    }

    public void publishPrivilegedEventToUser(UUID targetAdminUserId, AdminNotificationEvent event) {
        Objects.requireNonNull(targetAdminUserId, "targetAdminUserId no puede ser nulo");
        Objects.requireNonNull(event, "event no puede ser nulo");
        log.debug("Publicando evento privilegiado a admin {} en {} (tipo: {})",
                targetAdminUserId, USER_QUEUE_ADMIN, event.eventType());
        messagingTemplate.convertAndSendToUser(targetAdminUserId.toString(), USER_QUEUE_ADMIN, event);
    }
}
