package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByMerchantUserId(UUID merchantUserID);
    Optional<StoreEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByMerchantUserId(UUID merchantUserId);

    @Query("SELECT s FROM StoreEntity s WHERE " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.slug) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<StoreEntity> searchStores(
            @Param("query") String query,
            @Param("status") StoreStatus status
    );
}
