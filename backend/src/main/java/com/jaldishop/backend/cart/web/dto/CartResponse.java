package com.jaldishop.backend.cart.web.dto;

import com.jaldishop.backend.cart.application.CartView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record CartResponse(
        UUID id,
        UUID userId,
        UUID storeId,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        String currency,
        Instant updatedAt
) {
    public static CartResponse fromView(CartView view) {
        List<CartItemResponse> itemResponses = view.items() != null
                ? view.items().stream().map(CartItemResponse::fromView).collect(Collectors.toList())
                : List.of();

        return new CartResponse(
                view.id(),
                view.userId(),
                view.storeId(),
                itemResponses,
                view.totalItems(),
                view.totalAmount(),
                view.currency(),
                view.updatedAt()
        );
    }
}
