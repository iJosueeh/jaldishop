package com.jaldishop.backend.ordering.application.result;

import org.springframework.data.domain.Page;

import java.util.List;

public record MerchantOrderPageResponse(
        List<MerchantOrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static MerchantOrderPageResponse from(Page<MerchantOrderSummaryResponse> page) {
        if (page == null) {
            return new MerchantOrderPageResponse(List.of(), 0, 0, 0, 0, true, true);
        }
        return new MerchantOrderPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
