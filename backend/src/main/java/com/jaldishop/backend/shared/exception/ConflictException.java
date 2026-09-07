package com.jaldishop.backend.shared.exception;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super("RESOURCE_CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }

}
