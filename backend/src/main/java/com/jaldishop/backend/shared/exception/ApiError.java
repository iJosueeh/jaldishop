package com.jaldishop.backend.shared.exception;

public record ApiError(
   String code,
   String message
) {}
