package com.jaldishop.backend.cart.application;

import java.util.UUID;

public record ClearCartCommand(
        UUID userId,
        UUID storeId
) {}
