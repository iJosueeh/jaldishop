package com.jaldishop.backend.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SharedDomainModelTest {

    @Test
    @DisplayName("CustomerSnapshot debe recortar espacios y permitir valores")
    void testCustomerSnapshot() {
        CustomerSnapshot snapshot = CustomerSnapshot.of("  Juan Perez  ", " 987654321 ", " juan@example.com ");
        assertEquals("Juan Perez", snapshot.name());
        assertEquals("987654321", snapshot.phone());
        assertEquals("juan@example.com", snapshot.email());
    }

    @Test
    @DisplayName("DeliveryAddressSnapshot valida que coordenadas sean ambas nulas o ambas presentes")
    void testDeliveryAddressSnapshotCoordinates() {
        DeliveryAddressSnapshot validWithoutCoords = DeliveryAddressSnapshot.of("Av. Lima 123", "Frente al parque", null, null);
        assertFalse(validWithoutCoords.hasCoordinates());
        assertEquals("Av. Lima 123", validWithoutCoords.address());

        DeliveryAddressSnapshot validWithCoords = DeliveryAddressSnapshot.of(
                "Av. Lima 123", "Frente al parque",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793")
        );
        assertTrue(validWithCoords.hasCoordinates());

        assertThrows(IllegalArgumentException.class, () ->
                DeliveryAddressSnapshot.of("Av. Lima 123", null, new BigDecimal("-12.046374"), null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                DeliveryAddressSnapshot.of("Av. Lima 123", null, null, new BigDecimal("-77.042793"))
        );
    }

    @Test
    @DisplayName("MonetarySummary maneja valores por defecto y cálculos esperados")
    void testMonetarySummary() {
        MonetarySummary summary = MonetarySummary.of(
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                " PROMO10 ",
                new BigDecimal("8.00"),
                new BigDecimal("15.25"),
                new BigDecimal("98.00"),
                "pen"
        );

        assertEquals(new BigDecimal("100.00"), summary.subtotal());
        assertEquals(new BigDecimal("10.00"), summary.discount());
        assertEquals("PROMO10", summary.discountCode());
        assertEquals(new BigDecimal("8.00"), summary.deliveryFee());
        assertEquals(new BigDecimal("15.25"), summary.tax());
        assertEquals(new BigDecimal("98.00"), summary.total());
        assertEquals("PEN", summary.currency());
        assertTrue(summary.hasDiscount());
        assertEquals(new BigDecimal("98.00"), summary.calculateExpectedTotal());

        // Manejo de nulos
        MonetarySummary defaultSummary = MonetarySummary.of(null, null, "  ", null, null, null, null);
        assertEquals(BigDecimal.ZERO, defaultSummary.subtotal());
        assertEquals(BigDecimal.ZERO, defaultSummary.discount());
        assertNull(defaultSummary.discountCode());
        assertEquals(BigDecimal.ZERO, defaultSummary.deliveryFee());
        assertEquals(BigDecimal.ZERO, defaultSummary.tax());
        assertEquals(BigDecimal.ZERO, defaultSummary.total());
        assertEquals("PEN", defaultSummary.currency());
        assertFalse(defaultSummary.hasDiscount());
    }
}
