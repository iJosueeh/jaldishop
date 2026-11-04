package com.jaldishop.backend.inventory.infrastructure.persistence.adapter;

import com.jaldishop.backend.inventory.domain.model.Inventory;
import com.jaldishop.backend.inventory.domain.repository.InventoryRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;
    private final InventoryPersistenceMapper mapper;

    public InventoryRepositoryAdapter(InventoryJpaRepository jpaRepository, InventoryPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Inventory> findByVariantId(UUID variantId) {
        return jpaRepository.findById(variantId).map(mapper::toDomain);
    }

    @Override
    public Inventory save(Inventory inventory) {
        var entity = mapper.toEntity(inventory);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
