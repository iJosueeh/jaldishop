package com.jaldishop.backend.store.application.command;

import com.jaldishop.backend.store.domain.enums.StoreStatus;

import java.util.UUID;

public record ChangeStoreStatusCommand(
        UUID storeId,
        StoreStatus targetStatus,
        UUID adminId
) {
    public ChangeStoreStatusCommand(UUID storeId, StoreStatus targetStatus) {
        this(storeId, targetStatus, null);
    }
}
