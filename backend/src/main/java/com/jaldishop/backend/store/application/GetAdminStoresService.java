package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreOwnerResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminStoresService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public GetAdminStoresService(StoreRepository storeRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public List<AdminStoreSummaryResponse> listStores(String query, StoreStatus status) {
        List<Store> stores = storeRepository.findAllStores(query, status);

        return stores.stream()
                .map(this::mapToSummary)
                .toList();
    }

    public AdminStoreDetailResponse getStoreById(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + storeId));

        return mapToDetail(store);
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

    private AdminStoreSummaryResponse mapToSummary(Store store) {
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

    private AdminStoreDetailResponse mapToDetail(Store store) {
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
}
