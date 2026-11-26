package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.application.dto.PromoCodeCommand;
import com.jaldishop.backend.promo.application.dto.PromoCodeResponse;
import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageAdminPromoCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");

    @Mock
    private PromoCodeRepository repository;

    @Test
    void createsGlobalBannerPromoAndClearsPreviousBanners() {
        var service = new ManageAdminPromoCodeService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.existsByCodeIgnoreCase("JALDI10")).thenReturn(false);
        when(repository.save(any(PromoCode.class))).thenAnswer(inv -> inv.getArgument(0));

        PromoCodeCommand cmd = new PromoCodeCommand(
                "jaldi10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("30.00"),
                new BigDecimal("15.00"),
                200,
                null,
                null,
                true,
                true,
                "10% de descuento en tu primer pedido"
        );

        PromoCodeResponse response = service.create(cmd);

        assertThat(response.scope()).isEqualTo(PromoScope.GLOBAL);
        assertThat(response.fundingSource()).isEqualTo(PromoFundingSource.PLATFORM);
        assertThat(response.showInBanner()).isTrue();
        verify(repository).clearAllGlobalBannersExcept(eq(response.id()), eq(NOW));
    }

    @Test
    void rejectsDuplicateCodeCaseInsensitive() {
        var service = new ManageAdminPromoCodeService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.existsByCodeIgnoreCase("JALDI10")).thenReturn(true);

        PromoCodeCommand cmd = new PromoCodeCommand(
                "jaldi10", DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"),
                null, null, null, null, null, true, false, null
        );

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(ConflictException.class);
    }
}
