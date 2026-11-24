package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/public/platform-settings")
public class PublicPlatformSettingsController {

    private final PlatformSettingsProvider platformSettingsProvider;

    public PublicPlatformSettingsController(PlatformSettingsProvider platformSettingsProvider) {
        this.platformSettingsProvider = platformSettingsProvider;
    }

    @GetMapping
    public ResponseEntity<PublicPlatformSettingsResponse> getPublicSettings() {
        PlatformSettings settings = platformSettingsProvider != null
                ? platformSettingsProvider.getSettings()
                : PlatformSettings.defaultSettings();
        BigDecimal rate = settings != null && settings.customerServiceFeeRate() != null
                ? settings.customerServiceFeeRate()
                : BigDecimal.ZERO;
        BigDecimal cap = settings != null ? settings.customerServiceFeeCap() : null;
        return ResponseEntity.ok(new PublicPlatformSettingsResponse(rate, cap));
    }

    public record PublicPlatformSettingsResponse(BigDecimal customerServiceFeeRate, BigDecimal customerServiceFeeCap) {}
}
