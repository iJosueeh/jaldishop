package com.jaldishop.backend.cart.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Cart {

    private final UUID id;
    private final UUID userId;
    private final UUID storeId;
    private final List<CartItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    public Cart(UUID id, UUID userId, UUID storeId, List<CartItem> items, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del carrito es obligatorio");
        }
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio");
        }

        this.id = id;
        this.userId = userId;
        this.storeId = storeId;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Cart create(UUID userId, UUID storeId) {
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID(), userId, storeId, new ArrayList<>(), now, now);
    }

    public void addItem(UUID variantId, int quantity, BigDecimal referencePriceAmount, String referencePriceCurrency) {
        if (variantId == null) {
            throw new IllegalArgumentException("El ID de la variante es obligatorio");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        Optional<CartItem> existingItem = findItemByVariantId(variantId);
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.addQuantity(quantity);
            item.updateReferencePrice(referencePriceAmount, referencePriceCurrency);
        } else {
            CartItem newItem = CartItem.create(this.id, variantId, quantity, referencePriceAmount, referencePriceCurrency);
            this.items.add(newItem);
        }
        this.updatedAt = Instant.now();
    }

    public void updateItemQuantity(UUID variantId, int newQuantity) {
        if (variantId == null) {
            throw new IllegalArgumentException("El ID de la variante es obligatorio");
        }

        if (newQuantity <= 0) {
            removeItem(variantId);
            return;
        }

        Optional<CartItem> existingItem = findItemByVariantId(variantId);
        if (existingItem.isPresent()) {
            existingItem.get().updateQuantity(newQuantity);
            this.updatedAt = Instant.now();
        }
    }

    public void removeItem(UUID variantId) {
        if (variantId == null) {
            return;
        }
        boolean removed = this.items.removeIf(item -> item.getVariantId().equals(variantId));
        if (removed) {
            this.updatedAt = Instant.now();
        }
    }

    public void clear() {
        this.items.clear();
        this.updatedAt = Instant.now();
    }

    public Optional<CartItem> findItemByVariantId(UUID variantId) {
        if (variantId == null) {
            return Optional.empty();
        }
        return this.items.stream()
                .filter(item -> item.getVariantId().equals(variantId))
                .findFirst();
    }

    public int getTotalQuantity() {
        return this.items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal calculateTotalAmount() {
        return this.items.stream()
                .map(CartItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
