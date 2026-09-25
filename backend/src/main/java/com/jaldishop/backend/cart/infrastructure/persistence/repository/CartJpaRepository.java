package com.jaldishop.backend.cart.infrastructure.persistence.repository;

import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {

    Optional<CartEntity> findByUserIdAndStoreId(UUID userId, UUID storeId);
}
