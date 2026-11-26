package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.application.dto.PromoCodeCommand;
import com.jaldishop.backend.promo.application.dto.PromoCodeResponse;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ManageAdminPromoCodeService {

    private final PromoCodeRepository repository;
    private final Clock clock;

    public ManageAdminPromoCodeService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> listAllGlobal() {
        return repository.findByScopeOrderByCreatedAtDesc(PromoScope.GLOBAL).stream()
                .map(PromoCodeResponse::fromDomain)
                .toList();
    }

    public PromoCodeResponse create(PromoCodeCommand cmd) {
        String normalizedCode = PromoCode.normalizeCode(cmd.code());
        if (repository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ConflictException("PROMO_CODE_ALREADY_EXISTS", "Ya existe un código promocional con el código " + normalizedCode + ".");
        }

        Instant now = clock.instant();
        PromoCode promo = new PromoCode(
                UUID.randomUUID(),
                normalizedCode,
                PromoScope.GLOBAL,
                PromoFundingSource.PLATFORM,
                null,
                cmd.discountType(),
                cmd.discountValue(),
                cmd.minOrderAmount(),
                cmd.maxDiscountAmount(),
                cmd.usageLimit(),
                0,
                cmd.startsAt(),
                cmd.endsAt(),
                cmd.active(),
                cmd.showInBanner(),
                cmd.bannerText(),
                now,
                now
        );

        PromoCode saved = repository.save(promo);
        if (saved.isShowInBanner()) {
            repository.clearAllGlobalBannersExcept(saved.getId(), now);
        }
        return PromoCodeResponse.fromDomain(saved);
    }

    public PromoCodeResponse update(UUID id, PromoCodeCommand cmd) {
        PromoCode promo = loadGlobalOrThrow(id);
        String normalizedCode = PromoCode.normalizeCode(cmd.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new ConflictException("PROMO_CODE_ALREADY_EXISTS", "Ya existe otro código promocional con el código " + normalizedCode + ".");
        }

        Instant now = clock.instant();
        promo.update(
                normalizedCode,
                cmd.discountType(),
                cmd.discountValue(),
                cmd.minOrderAmount(),
                cmd.maxDiscountAmount(),
                cmd.usageLimit(),
                cmd.startsAt(),
                cmd.endsAt(),
                cmd.active(),
                cmd.showInBanner(),
                cmd.bannerText(),
                now
        );

        PromoCode saved = repository.save(promo);
        if (saved.isShowInBanner()) {
            repository.clearAllGlobalBannersExcept(saved.getId(), now);
        }
        return PromoCodeResponse.fromDomain(saved);
    }

    public PromoCodeResponse toggleActive(UUID id, boolean active) {
        PromoCode promo = loadGlobalOrThrow(id);
        promo.toggleActive(active, clock.instant());
        return PromoCodeResponse.fromDomain(repository.save(promo));
    }

    public void delete(UUID id) {
        PromoCode promo = loadGlobalOrThrow(id);
        repository.deleteById(promo.getId());
    }

    private PromoCode loadGlobalOrThrow(UUID id) {
        PromoCode promo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código promocional no encontrado."));
        if (promo.getScope() != PromoScope.GLOBAL) {
            throw new ResourceNotFoundException("Código promocional global no encontrado.");
        }
        return promo;
    }
}
