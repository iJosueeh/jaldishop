package com.jaldishop.backend.store.domain.event;

import java.util.UUID;

public record StoreCreatedDomainEvent(
        UUID storeId,
        UUID ownerUserId,
        String name,
        String slug
) {}
