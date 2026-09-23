package com.jaldishop.backend.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class VariantAttributeId implements Serializable {

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    public VariantAttributeId() {
    }

    public VariantAttributeId(UUID variantId, String name) {
        this.variantId = variantId;
        this.name = name;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantAttributeId that = (VariantAttributeId) o;
        return Objects.equals(variantId, that.variantId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId, name);
    }
}