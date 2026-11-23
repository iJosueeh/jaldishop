package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.port.in.UpdateStoreSettingsUseCase;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UpdateStoreSettingsService implements UpdateStoreSettingsUseCase {

    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public UpdateStoreSettingsService(StoreRepository storeRepository, StoreSettingsRepository storeSettingsRepository) {
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @Override
    public StoreSettingsResponse execute(UUID userId, StoreSettingsRequest request) {
        Store store = storeRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una tienda asociada a este usuario"));

        StoreSettings existing = storeSettingsRepository.findByStoreId(store.getId())
                .orElseGet(() -> StoreSettings.defaultSettings(store.getId()));

        StoreSettings updated = new StoreSettings(
                store.getId(),
                request.cancellationPolicy(),
                request.refundPolicy(),
                request.requireCustomerDni(),
                request.allowOrderNotes(),
                request.onlinePaymentsEnabled(),
                request.cashOnDeliveryEnabled(),
                request.acceptsPosOnDelivery(),
                request.minDeliveryAmount(),
                request.estimatedPrepTimeMinutes(),
                request.orderSoundAlert(),
                request.soundVolume(),
                request.lowStockVisualAlert(),
                existing.getCreatedAt() != null ? existing.getCreatedAt() : Instant.now(),
                Instant.now()
        );

        StoreSettings saved = storeSettingsRepository.save(updated);
        return StoreSettingsResponse.fromDomain(saved);
    }
}
