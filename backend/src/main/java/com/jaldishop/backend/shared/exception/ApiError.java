package com.jaldishop.backend.shared.exception;

import java.util.Map;

public record ApiError(
   String code,
   String message,
   Map<String, String> errors
) {
    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
