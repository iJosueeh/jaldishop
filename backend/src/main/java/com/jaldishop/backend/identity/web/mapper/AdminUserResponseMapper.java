package com.jaldishop.backend.identity.web.mapper;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserStoreSummaryResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AdminUserResponseMapper {

    private final StoreRepository storeRepository;

    public AdminUserResponseMapper(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public AdminUserSummaryResponse toSummary(User user) {
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

    public AdminUserDetailResponse toDetail(User user) {
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
