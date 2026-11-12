package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.time.LocalDate;

public record PeriodResponse(
        LocalDate from,
        LocalDate to,
        String timeZone
) {
    public static PeriodResponse from(PeriodFilter period) {
        return new PeriodResponse(period.fromDate(), period.toDate(), period.zoneId().getId());
    }
}
