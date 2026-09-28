package com.jaldishop.backend.capacity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityConfigurationTest {

    private final UUID storeId = UUID.randomUUID();

    @Test
    @DisplayName("Crear configuración con valores válidos (full day)")
    void createFullDay() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 1, null, null, 10);

        assertNotNull(config.getId());
        assertEquals(storeId, config.getStoreId());
        assertEquals(1, config.getDayOfWeek());
        assertNull(config.getStartTime());
        assertNull(config.getEndTime());
        assertEquals(10, config.getMaxCapacity());
        assertEquals(CapacityConfigurationStatus.ACTIVE, config.getStatus());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
        assertFalse(config.hasTimeSlot());
    }

    @Test
    @DisplayName("Crear configuración con franja horaria")
    void createTimeSlot() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 3, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertEquals(3, config.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), config.getStartTime());
        assertEquals(LocalTime.of(17, 0), config.getEndTime());
        assertEquals(20, config.getMaxCapacity());
        assertTrue(config.hasTimeSlot());
    }

    @Test
    @DisplayName("Lanzar excepción cuando storeId es nulo")
    void throwExceptionWhenStoreIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(null, 0, null, null, 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando dayOfWeek es menor a 0")
    void throwExceptionWhenDayOfWeekIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, -1, null, null, 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando dayOfWeek es mayor a 6")
    void throwExceptionWhenDayOfWeekExceeds6() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 7, null, null, 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando maxCapacity es negativo")
    void throwExceptionWhenMaxCapacityIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 0, null, null, -1));
    }

    @Test
    @DisplayName("Permitir maxCapacity en cero")
    void allowMaxCapacityZero() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 0, null, null, 0);
        assertEquals(0, config.getMaxCapacity());
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime tiene valor pero endTime es nulo")
    void throwExceptionWhenStartTimeWithoutEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 0, LocalTime.of(9, 0), null, 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando endTime tiene valor pero startTime es nulo")
    void throwExceptionWhenEndTimeWithoutStartTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 0, null, LocalTime.of(17, 0), 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime no es anterior a endTime")
    void throwExceptionWhenStartTimeNotBeforeEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 0, LocalTime.of(17, 0), LocalTime.of(9, 0), 5));
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime igual a endTime")
    void throwExceptionWhenStartTimeEqualsEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityConfiguration.create(storeId, 0, LocalTime.of(9, 0), LocalTime.of(9, 0), 5));
    }

    @Test
    @DisplayName("Activar y desactivar configuración")
    void activateAndDeactivate() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 0, null, null, 5);

        assertEquals(CapacityConfigurationStatus.ACTIVE, config.getStatus());

        config.deactivate();
        assertEquals(CapacityConfigurationStatus.INACTIVE, config.getStatus());

        config.activate();
        assertEquals(CapacityConfigurationStatus.ACTIVE, config.getStatus());
    }

    @Test
    @DisplayName("Actualizar configuración con valores válidos")
    void updateConfiguration() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 0, null, null, 5);
        Instant originalUpdatedAt = config.getUpdatedAt();

        config.update(3, LocalTime.of(8, 0), LocalTime.of(20, 0), 15);

        assertEquals(3, config.getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), config.getStartTime());
        assertEquals(LocalTime.of(20, 0), config.getEndTime());
        assertEquals(15, config.getMaxCapacity());
        assertTrue(config.getUpdatedAt().isAfter(originalUpdatedAt) || config.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    @DisplayName("Reconstituir configuración desde persistencia")
    void reconstituteConfiguration() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                id, storeId, 5, LocalTime.of(10, 0), LocalTime.of(14, 0), 8,
                CapacityConfigurationStatus.INACTIVE, createdAt, updatedAt);

        assertEquals(id, config.getId());
        assertEquals(storeId, config.getStoreId());
        assertEquals(5, config.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), config.getStartTime());
        assertEquals(LocalTime.of(14, 0), config.getEndTime());
        assertEquals(8, config.getMaxCapacity());
        assertEquals(CapacityConfigurationStatus.INACTIVE, config.getStatus());
        assertEquals(createdAt, config.getCreatedAt());
        assertEquals(updatedAt, config.getUpdatedAt());
    }

    @Test
    @DisplayName("Todos los días de la semana son válidos")
    void allDaysOfWeekAreValid() {
        for (int day = 0; day <= 6; day++) {
            CapacityConfiguration config = CapacityConfiguration.create(storeId, day, null, null, 5);
            assertEquals(day, config.getDayOfWeek());
        }
    }

    @Test
    @DisplayName("appliesTo - configuración full day aplica para cualquier franja del día")
    void appliesToFullDayMatchesAnySlot() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 1, null, null, 10);

        assertTrue(config.appliesTo(1, LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertTrue(config.appliesTo(1, LocalTime.of(20, 0), LocalTime.of(22, 0)));
    }

    @Test
    @DisplayName("appliesTo - rechaza día de la semana distinto")
    void appliesToRejectsDifferentDay() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 1, null, null, 10);

        assertFalse(config.appliesTo(2, LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada contenida en la franja configurada")
    void appliesToSlotContainsRequest() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertTrue(config.appliesTo(1, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("appliesTo - límite inferior: franja que inicia exactamente en startTime")
    void appliesToSlotBoundaryStartEqual() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertTrue(config.appliesTo(1, LocalTime.of(9, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("appliesTo - límite superior: franja que termina exactamente en endTime")
    void appliesToSlotBoundaryEndEqual() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertTrue(config.appliesTo(1, LocalTime.of(12, 0), LocalTime.of(17, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada antes del inicio de la configurada")
    void appliesToSlotRequestBeforeStart() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertFalse(config.appliesTo(1, LocalTime.of(8, 30), LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada después del fin de la configurada")
    void appliesToSlotRequestAfterEnd() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertFalse(config.appliesTo(1, LocalTime.of(17, 0), LocalTime.of(18, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada que cruza los límites de la configurada")
    void appliesToSlotRequestCrosses() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertFalse(config.appliesTo(1, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertFalse(config.appliesTo(1, LocalTime.of(16, 0), LocalTime.of(18, 0)));
    }

    @Test
    @DisplayName("appliesTo - configuración INACTIVE no aplica")
    void appliesToInactiveConfiguration() {
        CapacityConfiguration config = CapacityConfiguration.create(storeId, 1, null, null, 10);
        config.deactivate();

        assertFalse(config.appliesTo(1, LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja configurada no cubre una consulta de día completo")
    void appliesToSlotDoesNotMatchFullDayRequest() {
        CapacityConfiguration config = CapacityConfiguration.create(
                storeId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20);

        assertFalse(config.appliesTo(1, null, null));
    }
}
