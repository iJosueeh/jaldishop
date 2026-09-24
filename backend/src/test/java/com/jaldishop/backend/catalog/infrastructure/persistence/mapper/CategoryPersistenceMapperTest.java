package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryPersistenceMapperTest {

    private final CategoryPersistenceMapper mapper = new CategoryPersistenceMapper();

    @Test
    @DisplayName("Should map Domain Category to JPA CategoryEntity")
    void shouldMapDomainToEntity() {
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Instant now = Instant.now();

        Category domain = new Category(id, storeId, "Bebidas", "Jugos naturales", CategoryStatus.ACTIVE, now, now);
        CategoryEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(storeId, entity.getStoreId());
        assertEquals("Bebidas", entity.getName());
        assertEquals("Jugos naturales", entity.getDescription());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    @DisplayName("Should map JPA CategoryEntity to Domain Category")
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Instant now = Instant.now();

        CategoryEntity entity = new CategoryEntity(id, storeId, "Postres", "Tortas", "INACTIVE", now, now);
        Category domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(storeId, domain.getStoreId());
        assertEquals("Postres", domain.getName());
        assertEquals(CategoryStatus.INACTIVE, domain.getStatus());
    }
}