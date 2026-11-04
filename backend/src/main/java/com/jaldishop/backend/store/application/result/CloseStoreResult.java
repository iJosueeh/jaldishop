package com.jaldishop.backend.store.application.result;

import com.jaldishop.backend.store.domain.enums.CloseStoreAction;
import com.jaldishop.backend.store.domain.enums.StoreStatus;

import java.util.UUID;

public record CloseStoreResult(
        UUID storeId,
        CloseStoreAction action,
        StoreStatus storeStatus,
        String message
) {}
