package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.identity.domain.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        UserStatus status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt,
        UUID storeId,
        String storeName
) {}
