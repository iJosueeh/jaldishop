package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.UserStatus;

import java.util.UUID;

public record ChangeUserStatusCommand(
        UUID userId,
        UserStatus targetStatus,
        UUID currentAdminId
) {}
