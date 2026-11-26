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
public class ManageMerchantPromoCodeService {

    private final PromoCodeRepository repository;
    private final Clock clock;

    public ManageMerchantPromoCodeService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> listByStore(UUID storeId) {
        return repository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(PromoCodeResponse::fromDomain)
                .toList();
    }

    public PromoCodeResponse create(UUID storeId, PromoCodeCommand cmd) {
        String normalizedCode = PromoCode.normalizeCode(cmd.code());
        if (repository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ConflictException("PROMO_CODE_ALREADY_EXISTS", "Ya existe un código promocional con el código " + normalizedCode + ".");
        }

        Instant now = clock.instant();
        // Los comercios solo pueden crear cupones de tienda sin acceso al banner global
        PromoCode promo = new PromoCode(
                UUID.randomUUID(),
                normalizedCode,
                PromoScope.STORE,
                PromoFundingSource.MERCHANT,
                storeId,
                cmd.discountType(),
                cmd.discountValue(),
                cmd.minOrderAmount(),
                cmd.maxDiscountAmount(),
                cmd.usageLimit(),
                0,
                cmd.startsAt(),
                cmd.endsAt(),
                cmd.active(),
                false,
                null,
                now,
                now
        );
        return PromoCodeResponse.fromDomain(repository.save(promo));
    }

    public PromoCodeResponse update(UUID storeId, UUID promoId, PromoCodeCommand cmd) {
        PromoCode promo = loadStorePromoOrThrow(storeId, promoId);
        String normalizedCode = PromoCode.normalizeCode(cmd.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, promoId)) {
            throw new ConflictException("PROMO_CODE_ALREADY_EXISTS", "Ya existe otro código promocional con el código " + normalizedCode + ".");
        }

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
                false,
                null,
                clock.instant()
        );
        return PromoCodeResponse.fromDomain(repository.save(promo));
    }

    public PromoCodeResponse toggleActive(UUID storeId, UUID promoId, boolean active) {
        PromoCode promo = loadStorePromoOrThrow(storeId, promoId);
        promo.toggleActive(active, clock.instant());
        return PromoCodeResponse.fromDomain(repository.save(promo));
    }

    public void delete(UUID storeId, UUID promoId) {
        PromoCode promo = loadStorePromoOrThrow(storeId, promoId);
        repository.deleteById(promo.getId());
    }

    private PromoCode loadStorePromoOrThrow(UUID storeId, UUID promoId) {
        PromoCode promo = repository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado."));
        if (promo.getScope() != PromoScope.STORE || !storeId.equals(promo.getStoreId())) {
            throw new ResourceNotFoundException("Cupón no encontrado para esta tienda.");
        }
        return promo;
    }
}
