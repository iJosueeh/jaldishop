package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.*;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserStoreSummaryResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UpdateUserRolesService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;

    public UpdateUserRolesService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StoreRepository storeRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storeRepository = storeRepository;
    }

    public AdminUserDetailResponse updateRoles(UUID userId, Set<RoleName> roleNames, UUID currentAdminId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol asignado.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (userId.equals(currentAdminId) && !roleNames.contains(RoleName.ADMIN)) {
            throw new IllegalArgumentException("No puedes removerte tu propio rol de administrador.");
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en el sistema: " + roleName));
            resolvedRoles.add(role);
        }

        user.updateRoles(resolvedRoles);
        User savedUser = userRepository.save(user);

        return mapToDetail(savedUser);
    }

    private AdminUserDetailResponse mapToDetail(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        AdminUserStoreSummaryResponse storeSummary = null;

        if (user.hasRole(RoleName.MERCHANT)) {
            Optional<Store> store = storeRepository.findByMerchantUserId(user.getId());
            if (store.isPresent()) {
                Store s = store.get();
                storeSummary = new AdminUserStoreSummaryResponse(
                        s.getId(),
                        s.getName(),
                        s.getSlug(),
                        s.getStatus().name()
                );
            }
        }

        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                storeSummary
        );
    }
}
