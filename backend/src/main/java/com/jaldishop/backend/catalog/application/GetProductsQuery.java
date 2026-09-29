package com.jaldishop.backend.catalog.application;

import java.util.UUID;

public record GetProductsQuery(
        UUID storeId,
        UUID categoryId
) {}
