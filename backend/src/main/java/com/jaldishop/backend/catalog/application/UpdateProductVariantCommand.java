package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.VariantAttribute;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateProductVariantCommand(
        UUID variantId,
        UUID storeId,
        UUID productId,
        String presentationName,
        String sku,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean tracksInventory,
        String status,
        List<VariantAttribute> attributes
) {}