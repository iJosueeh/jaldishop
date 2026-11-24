package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.domain.repository.PlatformSettingsRepository;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.PlatformSettingsEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.repository.PlatformSettingsJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PlatformSettingsRepositoryAdapter implements PlatformSettingsRepository {

    private final PlatformSettingsJpaRepository jpaRepository;

    public PlatformSettingsRepositoryAdapter(PlatformSettingsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PlatformSettings> findSingleton() {
        return jpaRepository.findById(1).map(PlatformSettingsEntity::toDomain);
    }

    @Override
    public PlatformSettings save(PlatformSettings settings) {
        PlatformSettingsEntity entity = PlatformSettingsEntity.fromDomain(settings);
        return jpaRepository.save(entity).toDomain();
    }
}
