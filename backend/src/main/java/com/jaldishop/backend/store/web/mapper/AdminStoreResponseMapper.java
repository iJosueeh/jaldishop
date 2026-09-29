package com.jaldishop.backend.store.web.mapper;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreOwnerResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AdminStoreResponseMapper {

    private final UserRepository userRepository;

    public AdminStoreResponseMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminStoreSummaryResponse toSummary(Store store) {
        AdminStoreOwnerResponse merchant = fetchMerchant(store.getMerchantUserId());

        return new AdminStoreSummaryResponse(
                store.getId(),
                store.getMerchantUserId(),
                store.getName(),
                store.getSlug(),
                store.getContactPhone(),
                store.getStatus(),
                store.isDeliveryEnabled(),
                store.isPickupEnabled(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                merchant
        );
    }

    public AdminStoreDetailResponse toDetail(Store store) {
        AdminStoreOwnerResponse merchant = fetchMerchant(store.getMerchantUserId());

        return new AdminStoreDetailResponse(
                store.getId(),
                store.getMerchantUserId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getContactPhone(),
                store.getAddress(),
                store.getAddressReference(),
                store.getLatitude(),
                store.getLongitude(),
                store.isPickupEnabled(),
                store.isDeliveryEnabled(),
                store.getDeliveryFeeAmount(),
                store.getDeliveryFeeCurrency(),
                store.isTaxApplies(),
                store.getTaxRate(),
                store.getStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                merchant
        );
    }

    private AdminStoreOwnerResponse fetchMerchant(UUID merchantUserId) {
        if (merchantUserId == null) return null;
        Optional<User> userOpt = userRepository.findById(merchantUserId);
        return userOpt.map(u -> new AdminStoreOwnerResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone()
        )).orElse(null);
    }
}
