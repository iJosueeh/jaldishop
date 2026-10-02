package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartItem;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CheckoutItemSnapshotAssembler {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public CheckoutItemSnapshotAssembler(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<CheckoutItemSnapshot> assemble(Cart cart, UUID storeId) {
        if (cart == null || cart.getItems() == null) {
            return List.of();
        }

        List<CheckoutItemSnapshot> snapshots = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto no encontrada: " + item.getVariantId()));

            if (variant.getStatus() != VariantStatus.ACTIVE) {
                throw new ConflictException("VARIANT_INACTIVE",
                        "La presentación '" + variant.getPresentationName() + "' no se encuentra disponible.");
            }

            Product product = productRepository.findById(variant.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto asociado a la variante no encontrado."));

            if (!product.getStoreId().equals(storeId)) {
                throw new ConflictException("VARIANT_STORE_MISMATCH",
                        "El producto '" + product.getName() + "' no pertenece a esta tienda.");
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException("PRODUCT_INACTIVE",
                        "El producto '" + product.getName() + "' no se encuentra activo.");
            }

            String currency = variant.getPriceCurrency() != null ? variant.getPriceCurrency() : "PEN";
            BigDecimal subtotal = variant.getPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));

            snapshots.add(new CheckoutItemSnapshot(
                    variant.getId(),
                    product.getId(),
                    product.getName(),
                    variant.getPresentationName(),
                    variant.getSku(),
                    product.getImageUrl(),
                    item.getQuantity(),
                    variant.getPriceAmount(),
                    currency,
                    subtotal,
                    variant.isTracksInventory()
            ));
        }

        return snapshots;
    }
}
