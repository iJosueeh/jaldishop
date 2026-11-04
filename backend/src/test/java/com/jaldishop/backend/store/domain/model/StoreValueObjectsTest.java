package com.jaldishop.backend.store.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StoreValueObjectsTest {

    @Test
    @DisplayName("StoreLocation valida consistencia de latitud y longitud")
    void testStoreLocationCoordinates() {
        StoreLocation locWithoutCoords = StoreLocation.of("Av. Larco 100", "Miraflores", null, null);
        assertFalse(locWithoutCoords.hasCoordinates());
        assertEquals("Av. Larco 100", locWithoutCoords.address());
        assertEquals("Miraflores", locWithoutCoords.addressReference());

        StoreLocation locWithCoords = StoreLocation.of(
                "Av. Larco 100", "Miraflores",
                new BigDecimal("-12.122"), new BigDecimal("-77.030")
        );
        assertTrue(locWithCoords.hasCoordinates());

        assertThrows(IllegalArgumentException.class, () ->
                StoreLocation.of("Av. Larco 100", null, new BigDecimal("-12.122"), null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                StoreLocation.of("Av. Larco 100", null, null, new BigDecimal("-77.030"))
        );
    }

    @Test
    @DisplayName("DeliveryConfiguration valida rangos de impuestos y tarifa de delivery")
    void testDeliveryConfigurationValidation() {
        DeliveryConfiguration config = DeliveryConfiguration.of(
                true, true, new BigDecimal("10.00"), "pen", new BigDecimal("18.00")
        );

        assertTrue(config.pickupEnabled());
        assertTrue(config.deliveryEnabled());
        assertEquals(new BigDecimal("10.00"), config.deliveryFeeAmount());
        assertEquals("PEN", config.deliveryFeeCurrency());
        assertEquals(new BigDecimal("18.00"), config.taxRate());

        // Tarifa negativa no permitida cuando delivery está activo
        assertThrows(IllegalArgumentException.class, () ->
                DeliveryConfiguration.of(true, true, new BigDecimal("-1.00"), "PEN", new BigDecimal("18.00"))
        );

        // Impuesto inválido
        assertThrows(IllegalArgumentException.class, () ->
                DeliveryConfiguration.of(true, false, null, "PEN", new BigDecimal("-0.01"))
        );
        assertThrows(IllegalArgumentException.class, () ->
                DeliveryConfiguration.of(true, false, null, "PEN", new BigDecimal("100.01"))
        );

        // Valores nulos permitidos en persistencia
        DeliveryConfiguration defaults = DeliveryConfiguration.of(true, false, null, null, null);
        assertNull(defaults.deliveryFeeCurrency());
        assertNull(defaults.taxRate());
    }
}
