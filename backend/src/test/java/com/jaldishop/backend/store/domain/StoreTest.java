package com.jaldishop.backend.store.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {

    private final UUID merchantUserId = UUID.randomUUID();

    @Test
    @DisplayName("Crear tienda con valores válidos")
    void createStoreWithValidValues() {
        Store store = Store.create(
                merchantUserId,
                "  Super Tienda Jaldi  ",
                "  SUPER-TIENDA-JALDI  ",
                "Descripción de la tienda",
                "+50212345678",
                "Calle 1, Zona 1",
                "Frente al parque",
                new BigDecimal("14.634915"),
                new BigDecimal("-90.506882"),
                true,
                true,
                new BigDecimal("15.00"),
                "GTQ",
                true,
                new BigDecimal("12.00")
        );

        assertNotNull(store.getId());
        assertEquals(merchantUserId, store.getMerchantUserId());
        assertEquals("Super Tienda Jaldi", store.getName());
        assertEquals("super-tienda-jaldi", store.getSlug());
        assertEquals("Descripción de la tienda", store.getDescription());
        assertEquals("+50212345678", store.getContactPhone());
        assertEquals("Calle 1, Zona 1", store.getAddress());
        assertEquals("Frente al parque", store.getAddressReference());
        assertEquals(new BigDecimal("14.634915"), store.getLatitude());
        assertEquals(new BigDecimal("-90.506882"), store.getLongitude());
        assertTrue(store.isPickupEnabled());
        assertTrue(store.isDeliveryEnabled());
        assertEquals(new BigDecimal("15.00"), store.getDeliveryFeeAmount());
        assertEquals("GTQ", store.getDeliveryFeeCurrency());
        assertTrue(store.isTaxApplies());
        assertEquals(new BigDecimal("12.00"), store.getTaxRate());
        assertEquals(StoreStatus.ACTIVE, store.getStatus());
        assertNotNull(store.getCreatedAt());
        assertNotNull(store.getUpdatedAt());
    }

    @Test
    @DisplayName("Lanzar excepción cuando merchantUserId es nulo")
    void throwExceptionWhenMerchantUserIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        null,
                        "Mi Tienda",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null, false, null
                )
        );
        assertTrue(exception.getMessage().contains("ID del comerciante"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el nombre es nulo")
    void throwExceptionWhenNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        null,
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null, false, null
                )
        );
        assertTrue(exception.getMessage().contains("nombre"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el nombre está en blanco")
    void throwExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "   ",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null, false, null
                )
        );
        assertTrue(exception.getMessage().contains("nombre"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el slug es nulo")
    void throwExceptionWhenSlugIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        null,
                        null, null, null, null, null, null,
                        true, false, null, null, false, null
                )
        );
        assertTrue(exception.getMessage().contains("slug"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el slug está en blanco")
    void throwExceptionWhenSlugIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        "   ",
                        null, null, null, null, null, null,
                        true, false, null, null, false, null
                )
        );
        assertTrue(exception.getMessage().contains("slug"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando deliveryEnabled es true y la tarifa es negativa")
    void throwExceptionWhenDeliveryFeeIsNegative() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true,
                        true,
                        new BigDecimal("-0.01"),
                        "GTQ",
                        false,
                        null
                )
        );
        assertTrue(exception.getMessage().contains("tarifa de entrega"));
    }

    @Test
    @DisplayName("Permitir delivery deshabilitado con tarifa nula")
    void allowDeliveryDisabledWithNullFee() {
        Store store = Store.create(
                merchantUserId,
                "Mi Tienda",
                "mi-tienda",
                null, null, null, null, null, null,
                true,
                false,
                null,
                null,
                false,
                null
        );

        assertFalse(store.isDeliveryEnabled());
        assertNull(store.getDeliveryFeeAmount());
    }

    @Test
    @DisplayName("Lanzar excepción cuando taxRate es menor o igual a cero")
    void throwExceptionWhenTaxRateIsZeroOrNegative() {
        IllegalArgumentException exceptionZero = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null,
                        true,
                        BigDecimal.ZERO
                )
        );
        assertTrue(exceptionZero.getMessage().contains("tasa de impuestos"));

        IllegalArgumentException exceptionNegative = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null,
                        true,
                        new BigDecimal("-5.00")
                )
        );
        assertTrue(exceptionNegative.getMessage().contains("tasa de impuestos"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando taxRate excede 100")
    void throwExceptionWhenTaxRateExceeds100() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Store.create(
                        merchantUserId,
                        "Mi Tienda",
                        "mi-tienda",
                        null, null, null, null, null, null,
                        true, false, null, null,
                        true,
                        new BigDecimal("100.01")
                )
        );
        assertTrue(exception.getMessage().contains("tasa de impuestos"));
    }

    @Test
    @DisplayName("Actualizar información y configuración de la tienda")
    void updateStoreProfile() {
        Store store = Store.create(
                merchantUserId,
                "Tienda Original",
                "tienda-original",
                "Desc original",
                null, null, null, null, null,
                true, false, null, null, false, null
        );

        Instant originalUpdatedAt = store.getUpdatedAt();

        store.update(
                "Tienda Renovada",
                "Nueva descripción",
                "+50287654321",
                "Avenida Las Americas",
                "Junto al banco",
                new BigDecimal("14.600000"),
                new BigDecimal("-90.500000"),
                false,
                true,
                new BigDecimal("20.00"),
                "USD",
                true,
                new BigDecimal("15.00")
        );

        assertEquals("Tienda Renovada", store.getName());
        assertEquals("Nueva descripción", store.getDescription());
        assertEquals("+50287654321", store.getContactPhone());
        assertEquals("Avenida Las Americas", store.getAddress());
        assertEquals("Junto al banco", store.getAddressReference());
        assertEquals(new BigDecimal("14.600000"), store.getLatitude());
        assertEquals(new BigDecimal("-90.500000"), store.getLongitude());
        assertFalse(store.isPickupEnabled());
        assertTrue(store.isDeliveryEnabled());
        assertEquals(new BigDecimal("20.00"), store.getDeliveryFeeAmount());
        assertEquals("USD", store.getDeliveryFeeCurrency());
        assertTrue(store.isTaxApplies());
        assertEquals(new BigDecimal("15.00"), store.getTaxRate());
        assertTrue(store.getUpdatedAt().isAfter(originalUpdatedAt) || store.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    @DisplayName("Activar y desactivar tienda")
    void activateAndDeactivateStore() {
        Store store = Store.create(
                merchantUserId,
                "Tienda",
                "tienda",
                null, null, null, null, null, null,
                true, false, null, null, false, null
        );

        assertEquals(StoreStatus.ACTIVE, store.getStatus());

        store.deactivate();
        assertEquals(StoreStatus.INACTIVE, store.getStatus());

        store.activate();
        assertEquals(StoreStatus.ACTIVE, store.getStatus());
    }

    @Test
    @DisplayName("Reconstituir tienda desde persistencia")
    void reconstituteStore() {
        UUID storeId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        Store store = Store.reconstitute(
                storeId,
                merchantUserId,
                "Tienda Reconstituida",
                "tienda-reconstituida",
                "Desc",
                "+50211112222",
                "Direccion",
                "Ref",
                new BigDecimal("14.0"),
                new BigDecimal("-90.0"),
                true,
                true,
                new BigDecimal("10.00"),
                "GTQ",
                false,
                null,
                StoreStatus.SUSPENDED,
                createdAt,
                updatedAt
        );

        assertEquals(storeId, store.getId());
        assertEquals(merchantUserId, store.getMerchantUserId());
        assertEquals("Tienda Reconstituida", store.getName());
        assertEquals("tienda-reconstituida", store.getSlug());
        assertEquals(StoreStatus.SUSPENDED, store.getStatus());
        assertEquals(createdAt, store.getCreatedAt());
        assertEquals(updatedAt, store.getUpdatedAt());
    }
}
