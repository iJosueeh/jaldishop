package com.jaldishop.backend.promo.infrastructure.persistence.adapter;

import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import com.jaldishop.backend.promo.infrastructure.persistence.entity.PromoCodeEntity;
import com.jaldishop.backend.promo.infrastructure.persistence.repository.SpringDataPromoCodeRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPromoCodeRepositoryAdapter implements PromoCodeRepository {

    private final SpringDataPromoCodeRepository jpaRepository;

    public JpaPromoCodeRepositoryAdapter(SpringDataPromoCodeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PromoCode save(PromoCode promoCode) {
        PromoCodeEntity saved = jpaRepository.save(toEntity(promoCode));
        return toDomain(saved);
    }

    @Override
    public Optional<PromoCode> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PromoCode> findByCodeIgnoreCase(String code) {
        return jpaRepository.findByCodeIgnoreCase(code).map(this::toDomain);
    }

    @Override
    public boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID excludeId) {
        return jpaRepository.existsByCodeIgnoreCaseAndIdNot(code, excludeId);
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        return jpaRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public List<PromoCode> findByScopeOrderByCreatedAtDesc(PromoScope scope) {
        return jpaRepository.findByScopeOrderByCreatedAtDesc(scope).stream().map(this::toDomain).toList();
    }

    @Override
    public List<PromoCode> findByStoreIdOrderByCreatedAtDesc(UUID storeId) {
        return jpaRepository.findByStoreIdOrderByCreatedAtDesc(storeId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PromoCode> findActiveBannerPromo(Instant now) {
        return jpaRepository.findActiveBannerCandidates(now).stream().findFirst().map(this::toDomain);
    }

    @Override
    public void clearAllGlobalBannersExcept(UUID activePromoId, Instant now) {
        jpaRepository.clearAllGlobalBannersExcept(activePromoId, now);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int incrementUsedCountAtomically(String normalizedCode, Instant now) {
        return jpaRepository.incrementUsedCountAtomically(normalizedCode, now);
    }

    private PromoCodeEntity toEntity(PromoCode domain) {
        return new PromoCodeEntity(
                domain.getId(),
                domain.getCode(),
                domain.getScope(),
                domain.getFundingSource(),
                domain.getStoreId(),
                domain.getDiscountType(),
                domain.getDiscountValue(),
                domain.getMinOrderAmount(),
                domain.getMaxDiscountAmount(),
                domain.getUsageLimit(),
                domain.getUsedCount(),
                domain.getStartsAt(),
                domain.getEndsAt(),
                domain.isActive(),
                domain.isShowInBanner(),
                domain.getBannerText(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private PromoCode toDomain(PromoCodeEntity entity) {
        return new PromoCode(
                entity.getId(),
                entity.getCode(),
                entity.getScope(),
                entity.getFundingSource(),
                entity.getStoreId(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getMinOrderAmount(),
                entity.getMaxDiscountAmount(),
                entity.getUsageLimit(),
                entity.getUsedCount(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.isActive(),
                entity.isShowInBanner(),
                entity.getBannerText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
