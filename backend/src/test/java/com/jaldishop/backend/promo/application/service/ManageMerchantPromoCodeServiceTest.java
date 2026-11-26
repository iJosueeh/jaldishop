package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.application.dto.PromoCodeCommand;
import com.jaldishop.backend.promo.application.dto.PromoCodeResponse;
import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageMerchantPromoCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");

    @Mock
    private PromoCodeRepository repository;

    @Test
    void forcesStoreScopeMerchantFundingAndBlocksBanner() {
        var service = new ManageMerchantPromoCodeService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID storeId = UUID.randomUUID();
        when(repository.existsByCodeIgnoreCase("CAFE15")).thenReturn(false);
        when(repository.save(any(PromoCode.class))).thenAnswer(inv -> inv.getArgument(0));

        PromoCodeCommand cmd = new PromoCodeCommand(
                "cafe15",
                DiscountType.PERCENTAGE,
                new BigDecimal("15.00"),
                new BigDecimal("25.00"),
                null,
                50,
                null,
                null,
                true,
                true, // comerciante intenta activar banner global
                "Hack banner"
        );

        PromoCodeResponse response = service.create(storeId, cmd);

        assertThat(response.scope()).isEqualTo(PromoScope.STORE);
        assertThat(response.fundingSource()).isEqualTo(PromoFundingSource.MERCHANT);
        assertThat(response.storeId()).isEqualTo(storeId);
        assertThat(response.showInBanner()).isFalse();
        assertThat(response.bannerText()).isNull();
        assertThat(response.code()).isEqualTo("CAFE15");
    }

    @Test
    void preventsModifyingPromoOwnedByAnotherStore() {
        var service = new ManageMerchantPromoCodeService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID ownerStore = UUID.randomUUID();
        UUID otherStore = UUID.randomUUID();
        UUID promoId = UUID.randomUUID();

        PromoCode existing = new PromoCode(
                promoId, "STORE10", PromoScope.STORE, PromoFundingSource.MERCHANT, ownerStore,
                DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null, null, null, 0,
                null, null, true, false, null, NOW, NOW
        );
        when(repository.findById(promoId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.toggleActive(otherStore, promoId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
