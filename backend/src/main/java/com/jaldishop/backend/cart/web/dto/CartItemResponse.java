package com.jaldishop.backend.cart.web.dto;

import com.jaldishop.backend.cart.application.CartItemView;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String presentationName,
        String sku,
        String imageUrl,
        int quantity,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal subtotalAmount,
        boolean tracksInventory,
        boolean available
) {
    public static CartItemResponse fromView(CartItemView view) {
        return new CartItemResponse(
                view.variantId(),
                view.productId(),
                view.productName(),
                view.presentationName(),
                view.sku(),
                view.imageUrl(),
                view.quantity(),
                view.unitPriceAmount(),
                view.unitPriceCurrency(),
                view.subtotalAmount(),
                view.tracksInventory(),
                view.available()
        );
    }
}
