package com.jaldishop.backend.identity.application.command;

import com.jaldishop.backend.identity.domain.enums.UserStatus;

import java.util.UUID;

public record ChangeUserStatusCommand(
        UUID userId,
        UserStatus targetStatus,
        UUID currentAdminId
) {}
