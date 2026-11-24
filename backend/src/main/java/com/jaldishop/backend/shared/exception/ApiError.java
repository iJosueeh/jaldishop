package com.jaldishop.backend.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> metadata
) {
    public ApiError(String code, String message) {
        this(code, message, null, null);
    }

    public ApiError(String code, String message, Map<String, String> errors) {
        this(code, message, errors, null);
    }
}
