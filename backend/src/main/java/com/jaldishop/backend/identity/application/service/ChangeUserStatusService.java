package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ChangeUserStatusCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeUserStatusService {

    private final UserRepository userRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ChangeUserStatusService(
            UserRepository userRepository,
            AdminAuditService auditService
    ) {
        this(userRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ChangeUserStatusService(
            UserRepository userRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public User execute(ChangeUserStatusCommand command) {
        User targetUser = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + command.userId()));

        if (targetUser.isPrimaryAdmin() && command.targetStatus() == UserStatus.SUSPENDED) {
            throw new ConflictException("PRIMARY_ADMIN_CANNOT_BE_SUSPENDED", "El administrador principal no puede ser suspendido.");
        }

        if (targetUser.hasRole(RoleName.ADMIN)) {
            User actor = command.currentAdminId() != null
                    ? userRepository.findById(command.currentAdminId()).orElse(null)
                    : null;
            if (actor == null || !actor.isPrimaryAdmin()) {
                throw new AccessDeniedException("Solo el administrador principal puede modificar el estado de una cuenta de administrador.");
            }
        }

        if (command.targetStatus() == UserStatus.SUSPENDED) {
            targetUser.suspend(command.currentAdminId());
        } else if (command.targetStatus() == UserStatus.ACTIVE) {
            targetUser.activate();
        } else {
            throw new IllegalArgumentException("Estado no soportado para cambio de estado de usuario: " + command.targetStatus());
        }

        User saved = userRepository.save(targetUser);

        String actorEmail = command.currentAdminId() != null
                ? userRepository.findById(command.currentAdminId()).map(User::getEmail).orElse(null)
                : null;
        auditService.logSuccess(
                command.currentAdminId(),
                actorEmail,
                command.targetStatus() == UserStatus.SUSPENDED ? "SUSPEND_USER" : "ACTIVATE_USER",
                "USER",
                command.userId(),
                null,
                "Nuevo estado: " + command.targetStatus()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.UserStatusChangedDomainEvent(
                    saved.getId(),
                    command.targetStatus().name(),
                    command.currentAdminId()
            ));
        }

        return saved;
    }
}
