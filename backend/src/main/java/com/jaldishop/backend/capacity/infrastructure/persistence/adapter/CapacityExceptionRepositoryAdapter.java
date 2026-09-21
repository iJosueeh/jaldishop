package com.jaldishop.backend.capacity.infrastructure.persistence.adapter;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionRepository;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import com.jaldishop.backend.capacity.infrastructure.persistence.mapper.CapacityExceptionPersistenceMapper;
import com.jaldishop.backend.capacity.infrastructure.persistence.repository.CapacityExceptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CapacityExceptionRepositoryAdapter implements CapacityExceptionRepository {

    private final CapacityExceptionJpaRepository jpaRepository;
    private final CapacityExceptionPersistenceMapper mapper;

    public CapacityExceptionRepositoryAdapter(CapacityExceptionJpaRepository jpaRepository,
                                              CapacityExceptionPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CapacityException> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CapacityException> findByStoreId(UUID storeId) {
        return jpaRepository.findByStoreIdOrderByServiceDateDescStartTimeAsc(storeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CapacityException> findByStoreIdAndServiceDate(UUID storeId, LocalDate serviceDate) {
        return jpaRepository.findByStoreIdAndServiceDateOrderByStartTimeAsc(storeId, serviceDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CapacityException save(CapacityException exception) {
        CapacityExceptionEntity entity = mapper.toEntity(exception);
        CapacityExceptionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
