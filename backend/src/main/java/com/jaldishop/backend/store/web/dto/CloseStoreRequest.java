package com.jaldishop.backend.store.web.dto;

import jakarta.validation.constraints.Size;

public record CloseStoreRequest(
        @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
        String reason
) {
}
