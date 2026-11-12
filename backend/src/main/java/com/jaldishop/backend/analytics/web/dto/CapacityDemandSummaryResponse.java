package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.CapacityDemandSummary;

public record CapacityDemandSummaryResponse(
        long committedReservationsCount,
        long releasedReservationsCount,
        String notes
) {
    public static CapacityDemandSummaryResponse from(CapacityDemandSummary domain) {
        if (domain == null) {
            return null;
        }
        return new CapacityDemandSummaryResponse(
                domain.committedReservationsCount(),
                domain.releasedReservationsCount(),
                domain.notes()
        );
    }
}
