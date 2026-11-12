package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RevokeAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public RevokeAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminAuditService auditService
    ) {
        this(userRepository, roleRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RevokeAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public User execute(UUID targetUserId, UUID currentAdminId) {
        if (currentAdminId == null) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        User actor = userRepository.findById(currentAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado."));

        if (!actor.isPrimaryAdmin()) {
            throw new AccessDeniedException("Solo el administrador principal puede revocar privilegios administrativos.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + targetUserId));

        if (targetUser.isPrimaryAdmin()) {
            throw new ConflictException("PRIMARY_ADMIN_IMMUTABLE", "No se pueden revocar los privilegios del administrador principal.");
        }

        if (!targetUser.hasRole(RoleName.ADMIN)) {
            throw new ConflictException("USER_NOT_ADMIN", "El usuario no cuenta con el rol ADMIN.");
        }

        Set<Role> remainingRoles = targetUser.getRoles().stream()
                .filter(r -> r.getName() != RoleName.ADMIN)
                .collect(Collectors.toCollection(HashSet::new));

        if (remainingRoles.isEmpty()) {
            Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol CUSTOMER no encontrado."));
            remainingRoles.add(customerRole);
        }

        targetUser.updateRoles(remainingRoles);
        User saved = userRepository.save(targetUser);

        userRepository.incrementCredentialsVersion(targetUserId);

        auditService.logSuccess(
                actor.getId(),
                actor.getEmail(),
                "REVOKE_ADMIN",
                "USER",
                targetUserId,
                null,
                "Privilegio ADMIN revocado para: " + targetUser.getEmail()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.AdminRoleModifiedDomainEvent(
                    saved.getId(),
                    saved.getEmail(),
                    "REVOKED",
                    currentAdminId
            ));
        }

        return saved;
    }
}
