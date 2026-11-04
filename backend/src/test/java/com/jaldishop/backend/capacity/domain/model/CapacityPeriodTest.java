package com.jaldishop.backend.capacity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class CapacityPeriodTest {

    @Test
    @DisplayName("Crear período de jornada completa con allDay o nulos")
    void shouldCreateAllDayPeriod() {
        CapacityPeriod period = CapacityPeriod.allDay();

        assertNull(period.startTime());
        assertNull(period.endTime());
        assertTrue(period.isAllDay());
        assertFalse(period.hasTimeSlot());
    }

    @Test
    @DisplayName("Crear período válido con franja horaria")
    void shouldCreateValidTimeSlotPeriod() {
        CapacityPeriod period = CapacityPeriod.of(LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertEquals(LocalTime.of(9, 0), period.startTime());
        assertEquals(LocalTime.of(17, 0), period.endTime());
        assertFalse(period.isAllDay());
        assertTrue(period.hasTimeSlot());
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime está presente pero endTime es nulo")
    void shouldThrowWhenStartTimeWithoutEndTime() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new CapacityPeriod(LocalTime.of(9, 0), null));
        assertEquals("startTime y endTime deben ser ambos nulos o ambos con valor.", ex.getMessage());
    }

    @Test
    @DisplayName("Lanzar excepción cuando endTime está presente pero startTime es nulo")
    void shouldThrowWhenEndTimeWithoutStartTime() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new CapacityPeriod(null, LocalTime.of(17, 0)));
        assertEquals("startTime y endTime deben ser ambos nulos o ambos con valor.", ex.getMessage());
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime no es anterior a endTime")
    void shouldThrowWhenStartTimeNotBeforeEndTime() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new CapacityPeriod(LocalTime.of(17, 0), LocalTime.of(9, 0)));
        assertEquals("startTime debe ser anterior a endTime.", ex.getMessage());
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime es igual a endTime")
    void shouldThrowWhenStartTimeEqualsEndTime() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new CapacityPeriod(LocalTime.of(9, 0), LocalTime.of(9, 0)));
        assertEquals("startTime debe ser anterior a endTime.", ex.getMessage());
    }

    @Test
    @DisplayName("contains debe responder adecuadamente para jornada completa y rangos específicos")
    void shouldCheckContainmentCorrectly() {
        CapacityPeriod allDay = CapacityPeriod.allDay();
        assertTrue(allDay.contains(null, null));
        assertTrue(allDay.contains(LocalTime.of(8, 0), LocalTime.of(10, 0)));

        CapacityPeriod slot = CapacityPeriod.of(LocalTime.of(9, 0), LocalTime.of(13, 0));
        assertFalse(slot.contains(null, null));
        assertFalse(slot.contains(LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertFalse(slot.contains(LocalTime.of(10, 0), LocalTime.of(14, 0)));
        assertTrue(slot.contains(LocalTime.of(9, 0), LocalTime.of(13, 0)));
        assertTrue(slot.contains(LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }
}
