package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.identity.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        Instant createdAt
) {}
