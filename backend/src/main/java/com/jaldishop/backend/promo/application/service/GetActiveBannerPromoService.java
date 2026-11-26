package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.application.dto.ActiveBannerResponse;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional(readOnly = true)
public class GetActiveBannerPromoService {

    private final PromoCodeRepository repository;
    private final Clock clock;

    public GetActiveBannerPromoService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public ActiveBannerResponse getActiveBanner() {
        return repository.findActiveBannerPromo(clock.instant())
                .map(promo -> new ActiveBannerResponse(
                        true,
                        promo.getCode(),
                        promo.getBannerText(),
                        promo.getDiscountType(),
                        promo.getDiscountValue()
                ))
                .orElseGet(ActiveBannerResponse::empty);
    }
}
