package com.jaldishop.backend.identity.application.port;

import com.jaldishop.backend.identity.domain.model.PlatformSettings;

public interface PlatformSettingsProvider {
    PlatformSettings getSettings();
    void updateSettingsCache(PlatformSettings settings);
}
