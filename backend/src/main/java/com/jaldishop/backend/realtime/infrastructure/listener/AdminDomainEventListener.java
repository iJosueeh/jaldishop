package com.jaldishop.backend.realtime.infrastructure.listener;

import com.jaldishop.backend.identity.domain.event.AdminInvitationAcceptedDomainEvent;
import com.jaldishop.backend.identity.domain.event.AdminRoleModifiedDomainEvent;
import com.jaldishop.backend.identity.domain.event.UserRegisteredDomainEvent;
import com.jaldishop.backend.identity.domain.event.UserStatusChangedDomainEvent;
import com.jaldishop.backend.realtime.application.service.AdminRealtimePublisher;
import com.jaldishop.backend.realtime.domain.event.AdminNotificationEvent;
import com.jaldishop.backend.store.domain.event.StoreCreatedDomainEvent;
import com.jaldishop.backend.store.domain.event.StoreStatusChangedDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AdminDomainEventListener.class);

    private final AdminRealtimePublisher adminRealtimePublisher;

    public AdminDomainEventListener(AdminRealtimePublisher adminRealtimePublisher) {
        this.adminRealtimePublisher = adminRealtimePublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredDomainEvent event) {
        log.debug("Evento UserRegisteredDomainEvent recibido AFTER_COMMIT: {}", event.userId());
        try {
            adminRealtimePublisher.publishOperationalEvent(AdminNotificationEvent.of(
                    "USER_REGISTERED",
                    "USER",
                    event.userId(),
                    "CREATE",
                    "Nuevo usuario registrado con rol " + event.role()
            ));
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento operativo WebSocket USER_REGISTERED: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserStatusChanged(UserStatusChangedDomainEvent event) {
        log.debug("Evento UserStatusChangedDomainEvent recibido AFTER_COMMIT: {}", event.userId());
        try {
            adminRealtimePublisher.publishOperationalEvent(AdminNotificationEvent.of(
                    "USER_STATUS_CHANGED",
                    "USER",
                    event.userId(),
                    event.targetStatus(),
                    "Estado de usuario actualizado a " + event.targetStatus()
            ));
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento operativo WebSocket USER_STATUS_CHANGED: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStoreCreated(StoreCreatedDomainEvent event) {
        log.debug("Evento StoreCreatedDomainEvent recibido AFTER_COMMIT: {}", event.storeId());
        try {
            adminRealtimePublisher.publishOperationalEvent(AdminNotificationEvent.of(
                    "STORE_CREATED",
                    "STORE",
                    event.storeId(),
                    "CREATE",
                    "Nueva tienda creada: " + event.name()
            ));
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento operativo WebSocket STORE_CREATED: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStoreStatusChanged(StoreStatusChangedDomainEvent event) {
        log.debug("Evento StoreStatusChangedDomainEvent recibido AFTER_COMMIT: {}", event.storeId());
        try {
            adminRealtimePublisher.publishOperationalEvent(AdminNotificationEvent.of(
                    "STORE_STATUS_CHANGED",
                    "STORE",
                    event.storeId(),
                    event.targetStatus(),
                    "Estado de tienda actualizado a " + event.targetStatus()
            ));
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento operativo WebSocket STORE_STATUS_CHANGED: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminInvitationAccepted(AdminInvitationAcceptedDomainEvent event) {
        log.debug("Evento AdminInvitationAcceptedDomainEvent recibido AFTER_COMMIT: {}", event.userId());
        try {
            if (event.invitedBy() != null) {
                adminRealtimePublisher.publishPrivilegedEventToUser(event.invitedBy(), AdminNotificationEvent.of(
                        "ADMIN_INVITATION_ACCEPTED",
                        "ADMIN_INVITATION",
                        event.userId(),
                        "ACCEPTED",
                        "El usuario " + event.email() + " ha aceptado la invitación como administrador."
                ));
            }
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento privado WebSocket ADMIN_INVITATION_ACCEPTED: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminRoleModified(AdminRoleModifiedDomainEvent event) {
        log.debug("Evento AdminRoleModifiedDomainEvent recibido AFTER_COMMIT: {}", event.userId());
        try {
            if (event.performedBy() != null) {
                adminRealtimePublisher.publishPrivilegedEventToUser(event.performedBy(), AdminNotificationEvent.of(
                        "ADMIN_ROLE_MODIFIED",
                        "USER",
                        event.userId(),
                        event.action(),
                        "Privilegio administrativo " + event.action() + " para " + event.email()
                ));
            }
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento privado WebSocket ADMIN_ROLE_MODIFIED: {}", ex.getMessage());
        }
    }
}
