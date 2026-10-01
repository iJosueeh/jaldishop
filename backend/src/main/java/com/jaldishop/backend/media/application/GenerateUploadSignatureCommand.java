package com.jaldishop.backend.media.application;

import com.jaldishop.backend.media.domain.MediaTargetType;

import java.util.UUID;

public record GenerateUploadSignatureCommand(
        UUID userId,
        MediaTargetType targetType,
        UUID explicitStoreId
) {}
