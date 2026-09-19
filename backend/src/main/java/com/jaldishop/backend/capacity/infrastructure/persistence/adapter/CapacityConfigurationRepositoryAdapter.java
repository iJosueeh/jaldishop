package com.jaldishop.backend.capacity.infrastructure.persistence.adapter;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationRepository;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import com.jaldishop.backend.capacity.infrastructure.persistence.mapper.CapacityConfigurationPersistenceMapper;
import com.jaldishop.backend.capacity.infrastructure.persistence.repository.CapacityConfigurationJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CapacityConfigurationRepositoryAdapter implements CapacityConfigurationRepository {

    private final CapacityConfigurationJpaRepository jpaRepository;
    private final CapacityConfigurationPersistenceMapper mapper;

    public CapacityConfigurationRepositoryAdapter(CapacityConfigurationJpaRepository jpaRepository,
                                                  CapacityConfigurationPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CapacityConfiguration> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CapacityConfiguration> findByStoreId(UUID storeId) {
        return jpaRepository.findByStoreIdOrderByDayOfWeekAscStartTimeAsc(storeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsOverlappingByStoreIdAndDayOfWeek(UUID storeId, int dayOfWeek,
                                                          LocalTime startTime, LocalTime endTime,
                                                          UUID excludeId) {
        return !jpaRepository.existsNonOverlappingByStoreIdAndDayOfWeek(
                storeId, dayOfWeek, startTime, endTime, excludeId);
    }

    @Override
    public CapacityConfiguration save(CapacityConfiguration config) {
        CapacityConfigurationEntity entity = mapper.toEntity(config);
        CapacityConfigurationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
