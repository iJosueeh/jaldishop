package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class ConsumePromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(ConsumePromoCodeService.class);

    private final PromoCodeRepository repository;
    private final Clock clock;

    public ConsumePromoCodeService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Transactional
    public Optional<PromoFundingSource> consumeOnOrderConfirmation(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = PromoCode.normalizeCode(rawCode);
        int updatedRows = repository.incrementUsedCountAtomically(normalizedCode, clock.instant());
        if (updatedRows == 0) {
            log.warn("No se pudo incrementar atómicamente used_count para el código {} al confirmar orden ya pagada (límite alcanzado o desactivado concurrentemente).", normalizedCode);
        }
        return repository.findByCodeIgnoreCase(normalizedCode).map(PromoCode::getFundingSource);
    }
}
