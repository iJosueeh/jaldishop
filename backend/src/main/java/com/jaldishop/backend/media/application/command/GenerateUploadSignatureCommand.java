package com.jaldishop.backend.media.application.command;

import com.jaldishop.backend.media.domain.enums.MediaTargetType;

import java.util.UUID;
import java.util.Set;

public record GenerateUploadSignatureCommand(
        UUID userId,
        MediaTargetType targetType,
        UUID explicitStoreId,
        Set<String> roles
) {}
