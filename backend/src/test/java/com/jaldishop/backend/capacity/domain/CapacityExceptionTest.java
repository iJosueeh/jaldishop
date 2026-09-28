package com.jaldishop.backend.capacity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityExceptionTest {

    private final UUID storeId = UUID.randomUUID();

    @Test
    @DisplayName("Crear excepción full day exitosamente")
    void createFullDay() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 12, 25), null, null, 0, "Navidad");

        assertNotNull(exception.getId());
        assertEquals(storeId, exception.getStoreId());
        assertEquals(LocalDate.of(2026, 12, 25), exception.getServiceDate());
        assertNull(exception.getStartTime());
        assertNull(exception.getEndTime());
        assertEquals(0, exception.getExceptionCapacity());
        assertEquals("Navidad", exception.getReason());
        assertEquals(CapacityExceptionStatus.ACTIVE, exception.getStatus());
        assertNotNull(exception.getCreatedAt());
        assertNotNull(exception.getUpdatedAt());
        assertFalse(exception.hasTimeSlot());
    }

    @Test
    @DisplayName("Crear excepción con franja horaria")
    void createTimeSlot() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertEquals(5, exception.getExceptionCapacity());
        assertTrue(exception.hasTimeSlot());
        assertNull(exception.getReason());
    }

    @Test
    @DisplayName("Crear excepción sin razón")
    void createWithoutReason() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 1, 1), null, null, 20, null);

        assertNull(exception.getReason());
    }

    @Test
    @DisplayName("Lanzar excepción cuando storeId es nulo")
    void throwExceptionWhenStoreIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(null, LocalDate.of(2026, 1, 1), null, null, 5, null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando serviceDate es nulo")
    void throwExceptionWhenServiceDateIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, null, null, null, 5, null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando exceptionCapacity es negativo")
    void throwExceptionWhenCapacityIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, LocalDate.of(2026, 1, 1), null, null, -1, null));
    }

    @Test
    @DisplayName("Permitir exceptionCapacity en cero")
    void allowCapacityZero() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 1, 1), null, null, 0, null);
        assertEquals(0, exception.getExceptionCapacity());
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime tiene valor pero endTime es nulo")
    void throwExceptionWhenStartTimeWithoutEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, LocalDate.of(2026, 1, 1), LocalTime.of(9, 0), null, 5, null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando endTime tiene valor pero startTime es nulo")
    void throwExceptionWhenEndTimeWithoutStartTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, LocalDate.of(2026, 1, 1), null, LocalTime.of(17, 0), 5, null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime no es anterior a endTime")
    void throwExceptionWhenStartTimeNotBeforeEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, LocalDate.of(2026, 1, 1), LocalTime.of(17, 0), LocalTime.of(9, 0), 5, null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime igual a endTime")
    void throwExceptionWhenStartTimeEqualsEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityException.create(storeId, LocalDate.of(2026, 1, 1), LocalTime.of(9, 0), LocalTime.of(9, 0), 5, null));
    }

    @Test
    @DisplayName("Activar y desactivar excepción")
    void activateAndDeactivate() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 1, 1), null, null, 5, null);

        assertEquals(CapacityExceptionStatus.ACTIVE, exception.getStatus());

        exception.deactivate();
        assertEquals(CapacityExceptionStatus.INACTIVE, exception.getStatus());

        exception.activate();
        assertEquals(CapacityExceptionStatus.ACTIVE, exception.getStatus());
    }

    @Test
    @DisplayName("Actualizar excepción con valores válidos")
    void updateException() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 1, 1), null, null, 5, "Original");
        Instant originalUpdatedAt = exception.getUpdatedAt();

        exception.update(LocalDate.of(2026, 6, 20), LocalTime.of(8, 0), LocalTime.of(20, 0), 15, "Actualizada");

        assertEquals(LocalDate.of(2026, 6, 20), exception.getServiceDate());
        assertEquals(LocalTime.of(8, 0), exception.getStartTime());
        assertEquals(LocalTime.of(20, 0), exception.getEndTime());
        assertEquals(15, exception.getExceptionCapacity());
        assertEquals("Actualizada", exception.getReason());
        assertTrue(exception.getUpdatedAt().isAfter(originalUpdatedAt) || exception.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    @DisplayName("Reconstituir excepción desde persistencia")
    void reconstituteException() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");

        CapacityException exception = CapacityException.reconstitute(
                id, storeId, LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(17, 0),
                25, "Feriado", CapacityExceptionStatus.INACTIVE, createdAt, updatedAt);

        assertEquals(id, exception.getId());
        assertEquals(storeId, exception.getStoreId());
        assertEquals(LocalDate.of(2026, 8, 1), exception.getServiceDate());
        assertEquals(LocalTime.of(9, 0), exception.getStartTime());
        assertEquals(LocalTime.of(17, 0), exception.getEndTime());
        assertEquals(25, exception.getExceptionCapacity());
        assertEquals("Feriado", exception.getReason());
        assertEquals(CapacityExceptionStatus.INACTIVE, exception.getStatus());
        assertEquals(createdAt, exception.getCreatedAt());
        assertEquals(updatedAt, exception.getUpdatedAt());
    }

    @Test
    @DisplayName("appliesTo - excepción full day aplica para cualquier franja")
    void appliesToFullDayMatchesAnySlot() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 12, 25), null, null, 0, "Navidad");

        assertTrue(exception.appliesTo(LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertTrue(exception.appliesTo(LocalTime.of(20, 0), LocalTime.of(22, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada contenida en la franja de la excepción")
    void appliesToSlotContainsRequest() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertTrue(exception.appliesTo(LocalTime.of(11, 0), LocalTime.of(12, 0)));
        assertTrue(exception.appliesTo(LocalTime.of(10, 0), LocalTime.of(14, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada antes del inicio de la excepción")
    void appliesToSlotRequestBeforeStart() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertFalse(exception.appliesTo(LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada después del fin de la excepción")
    void appliesToSlotRequestAfterEnd() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertFalse(exception.appliesTo(LocalTime.of(14, 0), LocalTime.of(15, 0)));
    }

    @Test
    @DisplayName("appliesTo - franja solicitada que cruza los límites de la excepción")
    void appliesToSlotRequestCrosses() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertFalse(exception.appliesTo(LocalTime.of(9, 0), LocalTime.of(11, 0)));
        assertFalse(exception.appliesTo(LocalTime.of(13, 0), LocalTime.of(15, 0)));
    }

    @Test
    @DisplayName("appliesTo - excepción con franja no cubre una consulta de día completo")
    void appliesToSlotDoesNotMatchFullDayRequest() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null);

        assertFalse(exception.appliesTo(null, null));
    }

    @Test
    @DisplayName("appliesTo - excepción INACTIVE no aplica")
    void appliesToInactiveException() {
        CapacityException exception = CapacityException.create(
                storeId, LocalDate.of(2026, 7, 15), null, null, 5, null);
        exception.deactivate();

        assertFalse(exception.appliesTo(LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }
}
