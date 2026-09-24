package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductVariantRequest(
        @NotBlank(message = "Presentation name is required")
        @Size(max = 120, message = "Presentation name must not exceed 120 characters")
        String presentationName,

        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotNull(message = "Price amount is required")
        @DecimalMin(value = "0.01", message = "Price amount must be greater than zero")
        BigDecimal priceAmount,

        @NotBlank(message = "Price currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Price currency must be a 3-letter uppercase ISO code")
        String priceCurrency,

        boolean tracksInventory,

        String status,

        @Valid
        List<VariantAttributeDto> attributes
) {}