package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserStoreSummaryResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChangeUserStatusService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public ChangeUserStatusService(UserRepository userRepository, StoreRepository storeRepository) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
    }

    public AdminUserDetailResponse suspend(UUID userId, UUID currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        user.suspend(currentAdminId);
        User saved = userRepository.save(user);

        return mapToDetail(saved);
    }

    public AdminUserDetailResponse activate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        user.activate();
        User saved = userRepository.save(user);

        return mapToDetail(saved);
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
