package com.jaldishop.backend.catalog.domain;

public class VariantAttribute {
    private final String name;
    private final String value;

    public VariantAttribute(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attribute name cannot be empty");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Attribute value cannot be empty");
        }
        this.name = name.trim();
        this.value = value.trim();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}