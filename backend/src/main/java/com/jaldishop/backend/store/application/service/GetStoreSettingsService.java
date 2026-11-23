package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.port.in.GetStoreSettingsUseCase;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetStoreSettingsService implements GetStoreSettingsUseCase {

    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public GetStoreSettingsService(StoreRepository storeRepository, StoreSettingsRepository storeSettingsRepository) {
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @Override
    @Transactional
    public StoreSettingsResponse execute(UUID userId) {
        Store store = storeRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una tienda asociada a este usuario"));

        StoreSettings settings = storeSettingsRepository.findByStoreId(store.getId())
                .orElseGet(() -> storeSettingsRepository.save(StoreSettings.defaultSettings(store.getId())));

        return StoreSettingsResponse.fromDomain(settings);
    }
}
