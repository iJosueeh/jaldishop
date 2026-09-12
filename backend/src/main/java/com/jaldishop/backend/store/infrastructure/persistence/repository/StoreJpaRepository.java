package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByMerchantUserId(UUID merchantUserID);
    Optional<StoreEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByMerchantUserId(UUID merchantUserId);

}
