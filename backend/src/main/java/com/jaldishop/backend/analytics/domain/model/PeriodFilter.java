package com.jaldishop.backend.analytics.domain.model;

import com.jaldishop.backend.shared.exception.BusinessRuleException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Filtro temporal semiabierto [startInstant, endInstant) normalizado a America/Lima.
 */
public record PeriodFilter(
        LocalDate fromDate,
        LocalDate toDate,
        ZoneId zoneId
) {
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Lima");
    public static final int MAX_DAYS_RANGE = 90;

    public PeriodFilter {
        Objects.requireNonNull(fromDate, "La fecha de inicio (fromDate) es obligatoria.");
        Objects.requireNonNull(toDate, "La fecha de fin (toDate) es obligatoria.");
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio (" + fromDate + ") no puede ser posterior a la fecha de fin (" + toDate + ")."
            );
        }
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        if (daysBetween > MAX_DAYS_RANGE) {
            throw new IllegalArgumentException(
                    "El rango temporal máximo permitido para consultas analíticas es de " + MAX_DAYS_RANGE + " días."
            );
        }
    }

    public static PeriodFilter of(LocalDate fromDate, LocalDate toDate) {
        return new PeriodFilter(fromDate, toDate, DEFAULT_ZONE);
    }

    public static PeriodFilter of(LocalDate fromDate, LocalDate toDate, ZoneId zoneId) {
        return new PeriodFilter(fromDate, toDate, zoneId);
    }

    public Instant startInstant() {
        return fromDate.atStartOfDay(zoneId).toInstant();
    }

    /**
     * Límite superior semiabierto: primer instante del día siguiente a toDate.
     */
    public Instant endInstant() {
        return toDate.plusDays(1).atStartOfDay(zoneId).toInstant();
    }
}
