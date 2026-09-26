package com.jaldishop.backend.cart.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemView(
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
) {}
