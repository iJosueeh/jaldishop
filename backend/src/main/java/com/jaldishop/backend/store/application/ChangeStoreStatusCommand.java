package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreStatus;

import java.util.UUID;

public record ChangeStoreStatusCommand(
        UUID storeId,
        StoreStatus targetStatus
) {}
