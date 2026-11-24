package com.jaldishop.backend.shared.exception;

import java.util.Map;

public class ResourceConflictException extends ConflictException {

    private final Map<String, Object> metadata;

    public ResourceConflictException(String code, String message, Map<String, Object> metadata) {
        super(code, message);
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
