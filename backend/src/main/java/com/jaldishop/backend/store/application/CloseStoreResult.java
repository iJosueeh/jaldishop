package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.CloseStoreAction;
import com.jaldishop.backend.store.domain.StoreStatus;

import java.util.UUID;

public record CloseStoreResult(
        UUID storeId,
        CloseStoreAction action,
        StoreStatus storeStatus,
        String message
) {}
