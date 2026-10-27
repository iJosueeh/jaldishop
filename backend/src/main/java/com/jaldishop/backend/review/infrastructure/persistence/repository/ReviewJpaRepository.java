package com.jaldishop.backend.review.infrastructure.persistence.repository;

import com.jaldishop.backend.review.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    Optional<ReviewEntity> findByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.status = :status ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") String status);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
