package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserStoreSummaryResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAdminUsersService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public GetAdminUsersService(UserRepository userRepository, StoreRepository storeRepository) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
    }

    public List<AdminUserSummaryResponse> listUsers(String query, RoleName role, UserStatus status) {
        List<User> users = userRepository.findAll(query, role, status);

        return users.stream()
                .map(this::mapToSummary)
                .toList();
    }

    public AdminUserDetailResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        return mapToDetail(user);
    }

    private AdminUserSummaryResponse mapToSummary(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        UUID storeId = null;
        String storeName = null;

        if (user.hasRole(RoleName.MERCHANT)) {
            Optional<Store> store = storeRepository.findByMerchantUserId(user.getId());
            if (store.isPresent()) {
                storeId = store.get().getId();
                storeName = store.get().getName();
            }
        }

        return new AdminUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                storeId,
                storeName
        );
    }

    private AdminUserDetailResponse mapToDetail(User user) {
        Set<String> roleNames = user.getRoles().stream()
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
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                storeSummary
        );
    }
}
