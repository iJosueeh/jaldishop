package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreOwnerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ChangeStoreStatusService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public ChangeStoreStatusService(StoreRepository storeRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public AdminStoreDetailResponse suspend(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + storeId));

        store.suspend();
        Store saved = storeRepository.save(store);

        return mapToDetail(saved);
    }

    public AdminStoreDetailResponse activate(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + storeId));

        store.activate();
        Store saved = storeRepository.save(store);

        return mapToDetail(saved);
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
