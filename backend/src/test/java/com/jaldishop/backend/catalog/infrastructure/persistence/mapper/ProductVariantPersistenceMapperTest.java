package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.VariantAttribute;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantPersistenceMapperTest {

    private final ProductVariantPersistenceMapper mapper = new ProductVariantPersistenceMapper();

    @Test
    @DisplayName("Should correctly map variant with attributes to entity and back")
    void shouldMapVariantAndAttributes() {
        UUID variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        List<VariantAttribute> attributes = List.of(new VariantAttribute("Sabor", "Vainilla"));
        ProductVariant domain = new ProductVariant(
                variantId, productId, "Porción Mediana", "SKU-VAN-M",
                new BigDecimal("12.00"), "PEN", true, VariantStatus.ACTIVE, attributes, now, now
        );

        ProductVariantEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(variantId, entity.getId());
        assertEquals(1, entity.getAttributes().size());
        assertEquals("Sabor", entity.getAttributes().get(0).getId().getName());
        assertEquals("Vainilla", entity.getAttributes().get(0).getValue());

        ProductVariant mappedDomain = mapper.toDomain(entity);
        assertNotNull(mappedDomain);
        assertEquals(domain.getSku(), mappedDomain.getSku());
        assertEquals(1, mappedDomain.getAttributes().size());
        assertEquals("Vainilla", mappedDomain.getAttributes().get(0).getValue());
    }
}