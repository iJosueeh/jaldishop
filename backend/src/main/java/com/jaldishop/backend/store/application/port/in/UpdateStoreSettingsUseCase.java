package com.jaldishop.backend.store.application.port.in;

import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;

import java.util.UUID;

public interface UpdateStoreSettingsUseCase {
    StoreSettingsResponse execute(UUID userId, StoreSettingsRequest request);
}
