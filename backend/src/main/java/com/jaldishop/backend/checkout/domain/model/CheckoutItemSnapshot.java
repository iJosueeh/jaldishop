package com.jaldishop.backend.checkout.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutItemSnapshot(
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
