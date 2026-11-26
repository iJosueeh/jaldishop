package com.jaldishop.backend.promo.domain.repository;

import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository {

    PromoCode save(PromoCode promoCode);

    Optional<PromoCode> findById(UUID id);

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID excludeId);

    boolean existsByCodeIgnoreCase(String code);

    List<PromoCode> findByScopeOrderByCreatedAtDesc(PromoScope scope);

    List<PromoCode> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    Optional<PromoCode> findActiveBannerPromo(Instant now);

    void clearAllGlobalBannersExcept(UUID activePromoId, Instant now);

    void deleteById(UUID id);

    int incrementUsedCountAtomically(String normalizedCode, Instant now);
}
