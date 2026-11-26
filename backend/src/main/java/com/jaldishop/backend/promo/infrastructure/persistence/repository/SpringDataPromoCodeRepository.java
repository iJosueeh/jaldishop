package com.jaldishop.backend.promo.infrastructure.persistence.repository;

import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.infrastructure.persistence.entity.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPromoCodeRepository extends JpaRepository<PromoCodeEntity, UUID> {

    @Query("SELECT p FROM PromoCodeEntity p WHERE UPPER(p.code) = UPPER(:code)")
    Optional<PromoCodeEntity> findByCodeIgnoreCase(@Param("code") String code);

    @Query("SELECT COUNT(p) > 0 FROM PromoCodeEntity p WHERE UPPER(p.code) = UPPER(:code)")
    boolean existsByCodeIgnoreCase(@Param("code") String code);

    @Query("SELECT COUNT(p) > 0 FROM PromoCodeEntity p WHERE UPPER(p.code) = UPPER(:code) AND p.id <> :excludeId")
    boolean existsByCodeIgnoreCaseAndIdNot(@Param("code") String code, @Param("excludeId") UUID excludeId);

    List<PromoCodeEntity> findByScopeOrderByCreatedAtDesc(PromoScope scope);

    List<PromoCodeEntity> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    @Query("""
            SELECT p FROM PromoCodeEntity p
            WHERE p.scope = 'GLOBAL'
              AND p.active = true
              AND p.showInBanner = true
              AND (p.startsAt IS NULL OR p.startsAt <= :now)
              AND (p.endsAt IS NULL OR p.endsAt >= :now)
              AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
            ORDER BY p.updatedAt DESC
            """)
    List<PromoCodeEntity> findActiveBannerCandidates(@Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE PromoCodeEntity p
            SET p.showInBanner = false, p.updatedAt = :now
            WHERE p.scope = 'GLOBAL' AND p.showInBanner = true AND p.id <> :activeId
            """)
    void clearAllGlobalBannersExcept(@Param("activeId") UUID activeId, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE PromoCodeEntity p
            SET p.usedCount = p.usedCount + 1, p.updatedAt = :now
            WHERE UPPER(p.code) = UPPER(:code)
              AND p.active = true
              AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
            """)
    int incrementUsedCountAtomically(@Param("code") String code, @Param("now") Instant now);
}
