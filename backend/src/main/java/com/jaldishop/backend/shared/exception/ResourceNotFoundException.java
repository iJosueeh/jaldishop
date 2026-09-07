package com.jaldishop.backend.shared.exception;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s con identificador '%s' no fue encontrado", resourceName, identifier));
    }

}
