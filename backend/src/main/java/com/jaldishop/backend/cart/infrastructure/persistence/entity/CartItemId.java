package com.jaldishop.backend.cart.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartItemId implements Serializable {

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    public CartItemId() {
    }

    public CartItemId(UUID cartId, UUID variantId) {
        this.cartId = cartId;
        this.variantId = variantId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID cartId) {
        this.cartId = cartId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItemId that = (CartItemId) o;
        return Objects.equals(cartId, that.cartId) && Objects.equals(variantId, that.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, variantId);
    }
}
