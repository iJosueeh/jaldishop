package com.jaldishop.backend.store.domain.event;

import java.util.UUID;

public record StoreStatusChangedDomainEvent(
        UUID storeId,
        String targetStatus,
        UUID changedBy
) {}
