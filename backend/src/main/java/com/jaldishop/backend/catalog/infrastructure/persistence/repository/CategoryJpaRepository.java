package com.jaldishop.backend.catalog.infrastructure.persistence.repository;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByIdAndStoreId(UUID id, UUID storeId);
    List<CategoryEntity> findByStoreId(UUID storeId);
    boolean existsByStoreIdAndNameIgnoreCase(UUID storeId, String name);
    boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(UUID storeId, String name, UUID id);

    @Modifying
    @Query(value = "SET LOCAL lock_timeout = '2s'", nativeQuery = true)
    void applyShortLockTimeout();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.storeId = :storeId")
    Optional<CategoryEntity> findByIdAndStoreIdForUpdate(@Param("id") UUID id, @Param("storeId") UUID storeId);

    @Query(value = "SELECT COUNT(*) > 0 FROM products WHERE category_id = :categoryId", nativeQuery = true)
    boolean hasProducts(@Param("categoryId") UUID categoryId);

    @Modifying
    @Query(value = "DELETE FROM categories WHERE id = :id AND store_id = :storeId", nativeQuery = true)
    void deleteByIdAndStoreId(@Param("id") UUID id, @Param("storeId") UUID storeId);
}
