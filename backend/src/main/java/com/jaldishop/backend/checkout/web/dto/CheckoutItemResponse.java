package com.jaldishop.backend.checkout.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutItemResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String presentationName,
        String sku,
        String imageUrl,
        int quantity,
        BigDecimal unitPriceAmount,
        String currency,
        BigDecimal subtotalAmount,
        boolean tracksInventory
) {}
