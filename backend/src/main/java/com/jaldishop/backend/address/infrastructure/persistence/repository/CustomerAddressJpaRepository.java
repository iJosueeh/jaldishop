package com.jaldishop.backend.address.infrastructure.persistence.repository;

import com.jaldishop.backend.address.infrastructure.persistence.entity.CustomerAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressJpaRepository extends JpaRepository<CustomerAddressEntity, UUID> {

    @Query("SELECT a FROM CustomerAddressEntity a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<CustomerAddressEntity> findByUserId(@Param("userId") UUID userId);

    Optional<CustomerAddressEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<CustomerAddressEntity> findByUserIdAndIsDefaultTrue(UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE CustomerAddressEntity a SET a.isDefault = false, a.updatedAt = CURRENT_TIMESTAMP WHERE a.userId = :userId AND a.isDefault = true")
    int clearDefaultForUser(@Param("userId") UUID userId);

    @Query(value = "SELECT * FROM customer_addresses WHERE user_id = :userId ORDER BY updated_at DESC LIMIT 1", nativeQuery = true)
    Optional<CustomerAddressEntity> findMostRecentlyUpdated(@Param("userId") UUID userId);

    @Query(value = "SELECT id FROM users WHERE id = :userId FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockUserForUpdate(@Param("userId") UUID userId);
}
