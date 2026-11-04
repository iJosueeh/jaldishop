package com.jaldishop.backend.catalog.application.command;

import com.jaldishop.backend.catalog.domain.model.VariantAttribute;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductVariantCommand(
        UUID storeId,
        UUID productId,
        String presentationName,
        String sku,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean tracksInventory,
        List<VariantAttribute> attributes
) {}