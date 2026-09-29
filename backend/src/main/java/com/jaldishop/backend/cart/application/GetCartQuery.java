package com.jaldishop.backend.cart.application;

import java.util.UUID;

public record GetCartQuery(
        UUID userId,
        UUID storeId
) {}
