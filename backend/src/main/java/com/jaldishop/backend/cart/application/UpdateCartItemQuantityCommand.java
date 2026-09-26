package com.jaldishop.backend.cart.application;

import java.util.UUID;

public record UpdateCartItemQuantityCommand(
        UUID storeId,
        UUID variantId,
        int quantity
) {}
