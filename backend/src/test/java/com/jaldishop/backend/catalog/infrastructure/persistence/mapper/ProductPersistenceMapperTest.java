package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductPersistenceMapperTest {

    private final ProductPersistenceMapper mapper = new ProductPersistenceMapper();

    @Test
    @DisplayName("Should map Domain Product to JPA ProductEntity and vice versa")
    void shouldMapBetweenDomainAndEntity() {
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.now();

        Product domain = new Product(id, storeId, categoryId, "Cheesecake", "cheesecake", "De fresa", "http://img.png", ProductStatus.ACTIVE, now, now);
        ProductEntity entity = mapper.toEntity(domain);

        assertEquals(id, entity.getId());
        assertEquals(storeId, entity.getStoreId());
        assertEquals(categoryId, entity.getCategoryId());
        assertEquals("cheesecake", entity.getSlug());
        assertEquals("ACTIVE", entity.getStatus());

        Product remappedDomain = mapper.toDomain(entity);
        assertNotNull(remappedDomain);
        assertEquals(domain.getId(), remappedDomain.getId());
        assertEquals(domain.getName(), remappedDomain.getName());
        assertEquals(ProductStatus.ACTIVE, remappedDomain.getStatus());
    }
}