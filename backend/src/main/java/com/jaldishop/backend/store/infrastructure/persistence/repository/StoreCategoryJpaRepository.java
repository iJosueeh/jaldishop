package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreCategoryJpaRepository extends JpaRepository<StoreCategoryEntity, UUID> {

    Optional<StoreCategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT sc FROM StoreCategoryEntity sc WHERE (:status IS NULL OR sc.status = :status) ORDER BY sc.name ASC")
    List<StoreCategoryEntity> findAllByStatusOrderByNameAsc(@Param("status") String status);
}
