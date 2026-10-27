package com.jaldishop.backend.favorite.infrastructure.persistence.repository;

import com.jaldishop.backend.favorite.infrastructure.persistence.entity.FavoriteEntity;
import com.jaldishop.backend.favorite.infrastructure.persistence.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, FavoriteId> {

    @Query("SELECT f FROM FavoriteEntity f WHERE f.id.userId = :userId ORDER BY f.createdAt DESC")
    List<FavoriteEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) > 0 FROM FavoriteEntity f WHERE f.id.userId = :userId AND f.id.productId = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
