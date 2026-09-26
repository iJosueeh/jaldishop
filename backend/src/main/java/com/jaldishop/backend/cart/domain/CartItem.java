package com.jaldishop.backend.cart.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CartItem {

    private final UUID cartId;
    private final UUID variantId;
    private int quantity;
    private BigDecimal referencePriceAmount;
    private String referencePriceCurrency;
    private final Instant createdAt;
    private Instant updatedAt;

    public CartItem(UUID cartId, UUID variantId, int quantity,
                    BigDecimal referencePriceAmount, String referencePriceCurrency,
                    Instant createdAt, Instant updatedAt) {
        if (cartId == null) {
            throw new IllegalArgumentException("El ID del carrito es obligatorio");
        }
        if (variantId == null) {
            throw new IllegalArgumentException("El ID de la variante es obligatorio");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (referencePriceAmount == null || referencePriceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de referencia no puede ser negativo");
        }

        this.cartId = cartId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.referencePriceAmount = referencePriceAmount;
        this.referencePriceCurrency = (referencePriceCurrency != null && !referencePriceCurrency.isBlank())
                ? referencePriceCurrency.trim().toUpperCase() : "PEN";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static CartItem create(UUID cartId, UUID variantId, int quantity,
                                  BigDecimal referencePriceAmount, String referencePriceCurrency) {
        Instant now = Instant.now();
        return new CartItem(cartId, variantId, quantity, referencePriceAmount, referencePriceCurrency, now, now);
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.quantity = newQuantity;
        this.updatedAt = Instant.now();
    }

    public void addQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad a sumar debe ser mayor a cero");
        }
        this.quantity += additionalQuantity;
        this.updatedAt = Instant.now();
    }

    public void updateReferencePrice(BigDecimal priceAmount, String priceCurrency) {
        if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de referencia no puede ser negativo");
        }
        this.referencePriceAmount = priceAmount;
        if (priceCurrency != null && !priceCurrency.isBlank()) {
            this.referencePriceCurrency = priceCurrency.trim().toUpperCase();
        }
        this.updatedAt = Instant.now();
    }

    public BigDecimal calculateSubtotal() {
        return this.referencePriceAmount.multiply(BigDecimal.valueOf(this.quantity));
    }

    public UUID getCartId() {
        return cartId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getReferencePriceAmount() {
        return referencePriceAmount;
    }

    public String getReferencePriceCurrency() {
        return referencePriceCurrency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(cartId, cartItem.cartId) && Objects.equals(variantId, cartItem.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, variantId);
    }
}
