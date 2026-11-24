package com.jaldishop.backend.identity.domain.repository;

import com.jaldishop.backend.identity.domain.model.PlatformSettings;

import java.util.Optional;

public interface PlatformSettingsRepository {
    Optional<PlatformSettings> findSingleton();
    PlatformSettings save(PlatformSettings settings);
}
