package com.jaldishop.backend.cart.application.command;

import java.util.UUID;

public record RemoveCartItemCommand(
        UUID userId,
        UUID storeId,
        UUID variantId
) {}
