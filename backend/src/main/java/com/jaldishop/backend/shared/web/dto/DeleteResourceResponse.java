package com.jaldishop.backend.shared.web.dto;

import java.util.UUID;

public record DeleteResourceResponse(
        UUID id,
        String action,
        String message
) {
    public static DeleteResourceResponse deleted(UUID id, String message) {
        return new DeleteResourceResponse(id, "DELETED", message);
    }
}
