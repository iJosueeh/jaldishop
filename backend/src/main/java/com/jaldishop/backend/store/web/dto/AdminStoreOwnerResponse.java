package com.jaldishop.backend.store.web.dto;

import java.util.UUID;

public record AdminStoreOwnerResponse(
        UUID id,
        String email,
        String fullName,
        String phone
) {}
