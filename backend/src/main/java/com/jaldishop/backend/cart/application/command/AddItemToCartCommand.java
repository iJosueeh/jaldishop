package com.jaldishop.backend.cart.application.command;

import java.util.UUID;

public record AddItemToCartCommand(
        UUID userId,
        UUID storeId,
        UUID variantId,
        int quantity
) {}
