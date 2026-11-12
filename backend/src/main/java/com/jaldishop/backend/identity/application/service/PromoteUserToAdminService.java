package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PromoteUserToAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public PromoteUserToAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StoreRepository storeRepository,
            AdminAuditService auditService
    ) {
        this(userRepository, roleRepository, storeRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PromoteUserToAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StoreRepository storeRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storeRepository = storeRepository;
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
            throw new AccessDeniedException("Solo el administrador principal puede promover usuarios a administrador.");
        }

        if (targetUserId.equals(currentAdminId)) {
            throw new ConflictException("SELF_PROMOTION_FORBIDDEN", "No puedes autopromoverte.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + targetUserId));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("USER_NOT_ACTIVE", "Solo se pueden promover usuarios en estado ACTIVE.");
        }

        if (targetUser.hasRole(RoleName.ADMIN)) {
            throw new ConflictException("USER_ALREADY_ADMIN", "El usuario ya cuenta con el rol ADMIN.");
        }

        if (targetUser.hasRole(RoleName.MERCHANT) || storeRepository.findByOwnerUserId(targetUserId).isPresent()) {
            throw new ConflictException(
                    "MERCHANT_PROMOTION_RESTRICTED",
                    "No se permite la promoción directa de comerciantes con tiendas activas. Debe utilizarse una cuenta administrativa independiente mediante invitación."
            );
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Rol ADMIN no encontrado."));

        targetUser.addRole(adminRole);
        User saved = userRepository.save(targetUser);

        userRepository.incrementCredentialsVersion(targetUserId);

        auditService.logSuccess(
                actor.getId(),
                actor.getEmail(),
                "PROMOTE_TO_ADMIN",
                "USER",
                targetUserId,
                null,
                "Usuario promovido a ADMIN: " + targetUser.getEmail()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.AdminRoleModifiedDomainEvent(
                    saved.getId(),
                    saved.getEmail(),
                    "PROMOTED",
                    currentAdminId
            ));
        }

        return saved;
    }
}
