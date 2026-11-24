package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.infrastructure.persistence.entity.PlatformSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingsJpaRepository extends JpaRepository<PlatformSettingsEntity, Integer> {
}
