package com.jaldishop.backend.cart.web.dto;

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
) {}
