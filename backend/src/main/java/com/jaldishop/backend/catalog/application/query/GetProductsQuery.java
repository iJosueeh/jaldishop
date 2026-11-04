package com.jaldishop.backend.catalog.application.query;

import java.util.UUID;

public record GetProductsQuery(
        UUID storeId,
        UUID categoryId
) {}
