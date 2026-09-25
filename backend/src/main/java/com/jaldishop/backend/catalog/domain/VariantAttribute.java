package com.jaldishop.backend.catalog.domain;

public class VariantAttribute {
    private final String name;
    private final String value;

    public VariantAttribute(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del atributo no puede estar vacío");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El valor del atributo no puede estar vacío");
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