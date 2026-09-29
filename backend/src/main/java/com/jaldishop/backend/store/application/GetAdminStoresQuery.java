package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreStatus;

public record GetAdminStoresQuery(
        String query,
        StoreStatus status
) {}
