package com.jaldishop.backend.store.application.port.in;

import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;

import java.util.UUID;

public interface GetStoreSettingsUseCase {
    StoreSettingsResponse execute(UUID userId);
}
