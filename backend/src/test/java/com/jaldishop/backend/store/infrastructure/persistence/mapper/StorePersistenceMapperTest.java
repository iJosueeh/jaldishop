package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorePersistenceMapperTest {

    private StorePersistenceMapper mapper;

    private UUID testId;
    private UUID testMerchantUserId;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;

    @BeforeEach
    void setUp() {
        mapper = new StorePersistenceMapper();
        testId = UUID.randomUUID();
        testMerchantUserId = UUID.randomUUID();
        testCreatedAt = Instant.parse("2026-01-10T08:00:00Z");
        testUpdatedAt = Instant.parse("2026-01-10T12:00:00Z");
    }

    @Test
    @DisplayName("toEntity() debe mapear correctamente todos los campos desde el dominio")
    void toEntityShouldMapAllFields() {
        Store store = Store.reconstitute(
                testId,
                testMerchantUserId,
                "Tienda Jaldi",
                "tienda-jaldi",
                "Gran variedad de productos",
                "+51987654321",
                "Calle Real, Zona 1",
                "A la par de la farmacia",
                new BigDecimal("14.634915"),
                new BigDecimal("-90.506882"),
                true,
                true,
                new BigDecimal("15.50"),
                "PEN",
                new BigDecimal("18.00"),
                "https://example.com/logo.png",
                "https://example.com/banner.png",
                "https://instagram.com/tienda",
                "https://facebook.com/tienda",
                "+51987654321",
                StoreStatus.ACTIVE,
                testCreatedAt,
                testUpdatedAt
        );

        StoreEntity entity = mapper.toEntity(store);

        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testMerchantUserId, entity.getMerchantUserId());
        assertEquals("Tienda Jaldi", entity.getName());
        assertEquals("tienda-jaldi", entity.getSlug());
        assertEquals("Gran variedad de productos", entity.getDescription());
        assertEquals("+51987654321", entity.getContactPhone());
        assertEquals("Calle Real, Zona 1", entity.getAddress());
        assertEquals("A la par de la farmacia", entity.getAddressReference());
        assertEquals(new BigDecimal("14.634915"), entity.getLatitude());
        assertEquals(new BigDecimal("-90.506882"), entity.getLongitude());
        assertTrue(entity.isPickupEnabled());
        assertTrue(entity.isDeliveryEnabled());
        assertEquals(new BigDecimal("15.50"), entity.getDeliveryFeeAmount());
        assertEquals("PEN", entity.getDeliveryFeeCurrency());
        assertEquals(new BigDecimal("18.00"), entity.getTaxRate());
        assertEquals("https://example.com/logo.png", entity.getLogoUrl());
        assertEquals("https://example.com/banner.png", entity.getBannerUrl());
        assertEquals("https://instagram.com/tienda", entity.getInstagramUrl());
        assertEquals("https://facebook.com/tienda", entity.getFacebookUrl());
        assertEquals("+51987654321", entity.getWhatsappNumber());
        assertEquals(StoreStatus.ACTIVE, entity.getStatus());
        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testUpdatedAt, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("toDomain() debe mapear correctamente todos los campos desde la entidad JPA")
    void toDomainShouldMapAllFields() {
        StoreEntity entity = new StoreEntity(
                testId,
                testMerchantUserId,
                "Tienda Central",
                "tienda-central",
                "Venta de abarrotes",
                "+51987654321",
                "Avenida 5, Zona 10",
                "Frente al centro comercial",
                new BigDecimal("14.590000"),
                new BigDecimal("-90.510000"),
                false,
                true,
                new BigDecimal("25.00"),
                "PEN",
                new BigDecimal("18.00"),
                "https://example.com/logo.png",
                "https://example.com/banner.png",
                "https://instagram.com/tienda",
                "https://facebook.com/tienda",
                "+51987654321",
                StoreStatus.INACTIVE,
                testCreatedAt,
                testUpdatedAt
        );

        Store domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals(testMerchantUserId, domain.getMerchantUserId());
        assertEquals("Tienda Central", domain.getName());
        assertEquals("tienda-central", domain.getSlug());
        assertEquals("Venta de abarrotes", domain.getDescription());
        assertEquals("+51987654321", domain.getContactPhone());
        assertEquals("Avenida 5, Zona 10", domain.getAddress());
        assertEquals("Frente al centro comercial", domain.getAddressReference());
        assertEquals(new BigDecimal("14.590000"), domain.getLatitude());
        assertEquals(new BigDecimal("-90.510000"), domain.getLongitude());
        assertFalse(domain.isPickupEnabled());
        assertTrue(domain.isDeliveryEnabled());
        assertEquals(new BigDecimal("25.00"), domain.getDeliveryFeeAmount());
        assertEquals("PEN", domain.getDeliveryFeeCurrency());
        assertEquals(new BigDecimal("18.00"), domain.getTaxRate());
        assertEquals("https://example.com/logo.png", domain.getLogoUrl());
        assertEquals("https://example.com/banner.png", domain.getBannerUrl());
        assertEquals("https://instagram.com/tienda", domain.getInstagramUrl());
        assertEquals("https://facebook.com/tienda", domain.getFacebookUrl());
        assertEquals("+51987654321", domain.getWhatsappNumber());
        assertEquals(StoreStatus.INACTIVE, domain.getStatus());
        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertEquals(testUpdatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("toEntity() y toDomain() deben soportar campos opcionales nulos")
    void shouldHandleNullableFieldsBothWays() {
        Store store = Store.reconstitute(
                testId,
                testMerchantUserId,
                "Tienda Mínima",
                "tienda-minima",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                null,
                null,
                StoreStatus.ACTIVE,
                testCreatedAt,
                testUpdatedAt
        );

        StoreEntity entity = mapper.toEntity(store);
        assertNull(entity.getDescription());
        assertNull(entity.getContactPhone());
        assertNull(entity.getAddress());
        assertNull(entity.getAddressReference());
        assertNull(entity.getLatitude());
        assertNull(entity.getLongitude());
        assertNull(entity.getDeliveryFeeAmount());
        assertNull(entity.getDeliveryFeeCurrency());
        assertNull(entity.getTaxRate());

        Store domainFromEntity = mapper.toDomain(entity);
        assertNull(domainFromEntity.getDescription());
        assertNull(domainFromEntity.getContactPhone());
        assertNull(domainFromEntity.getAddress());
        assertNull(domainFromEntity.getAddressReference());
        assertNull(domainFromEntity.getLatitude());
        assertNull(domainFromEntity.getLongitude());
        assertNull(domainFromEntity.getDeliveryFeeAmount());
        assertNull(domainFromEntity.getDeliveryFeeCurrency());
        assertNull(domainFromEntity.getTaxRate());
    }
}
