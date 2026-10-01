package com.jaldishop.backend.media.web.dto;

import com.jaldishop.backend.media.domain.MediaTargetType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateSignatureRequest(
        @NotNull(message = "El tipo de destino multimedia es obligatorio")
        MediaTargetType targetType,
        UUID storeId
) {}
