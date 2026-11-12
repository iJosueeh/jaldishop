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
public class UpdateUserRolesService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditService auditService;

    public UpdateUserRolesService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    public User execute(UUID userId, Set<RoleName> roleNames, UUID currentAdminId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol asignado.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (targetUser.isPrimaryAdmin()) {
            throw new ConflictException("PRIMARY_ADMIN_IMMUTABLE", "No se pueden modificar los roles del administrador principal.");
        }

        boolean targetIsAdmin = targetUser.hasRole(RoleName.ADMIN);
        boolean requestingAdmin = roleNames.contains(RoleName.ADMIN);

        if (!targetIsAdmin && requestingAdmin) {
            throw new AccessDeniedException("No se puede asignar el rol ADMIN mediante actualización genérica de roles. Utilice el flujo de invitación o promoción administrativa.");
        }

        if (targetIsAdmin && !requestingAdmin) {
            throw new AccessDeniedException("No se puede revocar el rol ADMIN mediante actualización genérica de roles. Utilice el caso de uso de revocación administrativa.");
        }

        if (targetIsAdmin) {
            throw new AccessDeniedException("No se pueden modificar los roles de un administrador mediante este endpoint.");
        }

        if (currentAdminId != null && userId.equals(currentAdminId) && !roleNames.contains(RoleName.ADMIN)) {
            throw new IllegalArgumentException("No puedes removerte tu propio rol de administrador.");
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en el sistema: " + roleName));
            resolvedRoles.add(role);
        }

        targetUser.updateRoles(resolvedRoles);
        User saved = userRepository.save(targetUser);

        String actorEmail = currentAdminId != null ? userRepository.findById(currentAdminId).map(User::getEmail).orElse(null) : null;
        String assignedRoles = roleNames.stream().map(Enum::name).collect(Collectors.joining(","));
        auditService.logSuccess(
                currentAdminId,
                actorEmail,
                "UPDATE_USER_ROLES",
                "USER",
                userId,
                null,
                "Roles asignados: " + assignedRoles
        );

        return saved;
    }
}
