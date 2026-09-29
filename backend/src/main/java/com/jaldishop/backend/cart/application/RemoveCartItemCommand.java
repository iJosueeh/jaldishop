package com.jaldishop.backend.cart.application;

import java.util.UUID;

public record RemoveCartItemCommand(
        UUID userId,
        UUID storeId,
        UUID variantId
) {}
