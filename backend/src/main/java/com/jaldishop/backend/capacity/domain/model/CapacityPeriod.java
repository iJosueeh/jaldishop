package com.jaldishop.backend.capacity.domain.model;

import java.time.LocalTime;

/**
 * Value Object inmutable que representa un rango o período horario de capacidad.
 * Encapsula la regla de coherencia temporal donde ambos límites son nulos
 * (representando jornada completa) o ambos son no nulos con {@code startTime < endTime}.
 */
public record CapacityPeriod(LocalTime startTime, LocalTime endTime) {

    public CapacityPeriod {
        if ((startTime == null && endTime != null) || (startTime != null && endTime == null)) {
            throw new IllegalArgumentException("startTime y endTime deben ser ambos nulos o ambos con valor.");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime.");
        }
    }

    public static CapacityPeriod allDay() {
        return new CapacityPeriod(null, null);
    }

    public static CapacityPeriod of(LocalTime startTime, LocalTime endTime) {
        return new CapacityPeriod(startTime, endTime);
    }

    public boolean isAllDay() {
        return startTime == null && endTime == null;
    }

    public boolean hasTimeSlot() {
        return !isAllDay();
    }

    public boolean contains(LocalTime reqStartTime, LocalTime reqEndTime) {
        if (isAllDay()) {
            return true;
        }
        if (reqStartTime == null || reqEndTime == null) {
            return false;
        }
        return !reqStartTime.isBefore(startTime) && !reqEndTime.isAfter(endTime);
    }
}
