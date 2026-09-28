package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.*;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UpdateUserRolesService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UpdateUserRolesService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User execute(UUID userId, Set<RoleName> roleNames, UUID currentAdminId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol asignado.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (currentAdminId != null && userId.equals(currentAdminId) && !roleNames.contains(RoleName.ADMIN)) {
            throw new IllegalArgumentException("No puedes removerte tu propio rol de administrador.");
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en el sistema: " + roleName));
            resolvedRoles.add(role);
        }

        user.updateRoles(resolvedRoles);
        return userRepository.save(user);
    }
}
