package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminInvitationResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        AdminInvitationStatus status,
        Instant expiresAt,
        Instant createdAt
) {}
