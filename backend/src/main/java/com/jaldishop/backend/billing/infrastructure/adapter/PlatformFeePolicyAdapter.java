package com.jaldishop.backend.billing.infrastructure.adapter;

import com.jaldishop.backend.billing.application.port.out.FeePolicyProvider;
import com.jaldishop.backend.billing.domain.model.FeePolicy;
import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PlatformFeePolicyAdapter implements FeePolicyProvider {

    private final PlatformSettingsProvider platformSettingsProvider;

    public PlatformFeePolicyAdapter(PlatformSettingsProvider platformSettingsProvider) {
        this.platformSettingsProvider = platformSettingsProvider;
    }

    @Override
    public FeePolicy getEffectivePolicy() {
        if (platformSettingsProvider == null) {
            return FeePolicy.defaultPeruCommerce();
        }
        PlatformSettings settings = platformSettingsProvider.getSettings();
        if (settings == null || settings.marketplaceCommissionRate() == null) {
            return FeePolicy.defaultPeruCommerce();
        }

        BigDecimal takeRatePct = settings.marketplaceCommissionRate()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal cashTakeRatePct = takeRatePct
                .subtract(new BigDecimal("0.0150"))
                .max(BigDecimal.ZERO);

        return new FeePolicy(
                "DYNAMIC_PLATFORM_POLICY",
                "Política Dinámica de la Plataforma",
                takeRatePct,
                cashTakeRatePct,
                new BigDecimal("0.90"),
                new BigDecimal("60.00"),
                new BigDecimal("0.1800"),
                true
        );
    }
}
