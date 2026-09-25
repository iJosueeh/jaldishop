package com.jaldishop.backend.cart.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {

    @EmbeddedId
    private CartItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cartId")
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reference_price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal referencePriceAmount;

    @Column(name = "reference_price_currency", nullable = false, length = 3)
    private String referencePriceCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CartItemEntity() {
    }

    public CartItemEntity(CartItemId id, CartEntity cart, int quantity,
                          BigDecimal referencePriceAmount, String referencePriceCurrency,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.cart = cart;
        this.quantity = quantity;
        this.referencePriceAmount = referencePriceAmount;
        this.referencePriceCurrency = referencePriceCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CartItemId getId() {
        return id;
    }

    public void setId(CartItemId id) {
        this.id = id;
    }

    public CartEntity getCart() {
        return cart;
    }

    public void setCart(CartEntity cart) {
        this.cart = cart;
    }

    public UUID getCartId() {
        return id != null ? id.getCartId() : null;
    }

    public UUID getVariantId() {
        return id != null ? id.getVariantId() : null;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getReferencePriceAmount() {
        return referencePriceAmount;
    }

    public void setReferencePriceAmount(BigDecimal referencePriceAmount) {
        this.referencePriceAmount = referencePriceAmount;
    }

    public String getReferencePriceCurrency() {
        return referencePriceCurrency;
    }

    public void setReferencePriceCurrency(String referencePriceCurrency) {
        this.referencePriceCurrency = referencePriceCurrency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
