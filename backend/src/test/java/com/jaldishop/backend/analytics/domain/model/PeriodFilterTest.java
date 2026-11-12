package com.jaldishop.backend.analytics.domain.model;

import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodFilterTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Test
    @DisplayName("Debe construir intervalo semiabierto [start, end) correcto en America/Lima")
    void shouldBuildHalfOpenIntervalCorrectly() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        PeriodFilter filter = PeriodFilter.of(from, to, LIMA);

        Instant expectedStart = from.atStartOfDay(LIMA).toInstant();
        Instant expectedEnd = to.plusDays(1).atStartOfDay(LIMA).toInstant();

        assertThat(filter.startInstant()).isEqualTo(expectedStart);
        assertThat(filter.endInstant()).isEqualTo(expectedEnd);
        assertThat(filter.startInstant()).isBefore(filter.endInstant());
    }

    @Test
    @DisplayName("Debe lanzar excepción si fromDate es posterior a toDate")
    void shouldThrowWhenFromDateAfterToDate() {
        LocalDate from = LocalDate.of(2026, 11, 10);
        LocalDate to = LocalDate.of(2026, 11, 5);

        assertThatThrownBy(() -> PeriodFilter.of(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser posterior a la fecha de fin");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el rango supera los 90 días")
    void shouldThrowWhenRangeExceedsMaxDays() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 15); // > 90 days

        assertThatThrownBy(() -> PeriodFilter.of(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El rango temporal máximo permitido para consultas analíticas es de 90 días");
    }

    @Test
    @DisplayName("Debe admitir un solo día como intervalo semiabierto válido de 24 horas")
    void shouldSupportSingleDayInterval() {
        LocalDate singleDay = LocalDate.of(2026, 10, 15);

        PeriodFilter filter = PeriodFilter.of(singleDay, singleDay, LIMA);

        Instant start = filter.startInstant();
        Instant end = filter.endInstant();

        // 24 horas exactas (86400 segundos)
        assertThat(end.getEpochSecond() - start.getEpochSecond()).isEqualTo(86400L);
    }
}
