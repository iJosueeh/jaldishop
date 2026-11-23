package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoreSettingsJpaRepository extends JpaRepository<StoreSettingsEntity, UUID> {
}
