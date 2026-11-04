package com.jaldishop.backend.store.application.query;

import com.jaldishop.backend.store.domain.enums.StoreStatus;

public record GetAdminStoresQuery(
        String query,
        StoreStatus status
) {}
