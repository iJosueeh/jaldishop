package com.jaldishop.backend.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "variant_attributes")
public class VariantAttributeEntity {

    @EmbeddedId
    private VariantAttributeId id;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    public VariantAttributeEntity() {
    }

    public VariantAttributeEntity(VariantAttributeId id, String value) {
        this.id = id;
        this.value = value;
    }

    public VariantAttributeId getId() {
        return id;
    }

    public void setId(VariantAttributeId id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}