package com.jaldishop.backend.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "El token de verificación es obligatorio.")
        String token
) {}
