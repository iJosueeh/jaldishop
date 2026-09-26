package com.jaldishop.backend.cart.application;

import java.util.UUID;

public record AddItemToCartCommand(
        UUID storeId,
        UUID variantId,
        int quantity
) {}
