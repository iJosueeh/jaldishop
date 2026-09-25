package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartItem;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartViewAssembler {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    public CartViewAssembler(ProductVariantRepository productVariantRepository, ProductRepository productRepository) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
    }

    public CartView empty(UUID userId, UUID storeId) {
        return new CartView(
                null,
                userId,
                storeId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );
    }

    public CartView assemble(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemView> itemViews = new ArrayList<>();
        String currency = "PEN";

        for (CartItem item : cart.getItems()) {
            Optional<ProductVariant> variantOpt = productVariantRepository.findById(item.getVariantId());

            if (variantOpt.isPresent()) {
                ProductVariant variant = variantOpt.get();
                Optional<Product> productOpt = productRepository.findById(variant.getProductId());

                String productName = productOpt.map(Product::getName).orElse("Producto");
                String imageUrl = productOpt.map(Product::getImageUrl).orElse(null);
                UUID productId = variant.getProductId();
                boolean available = variant.getStatus() == VariantStatus.ACTIVE
                        && productOpt.map(p -> p.getStatus() == ProductStatus.ACTIVE).orElse(false);

                currency = variant.getPriceCurrency();
                BigDecimal subtotal = variant.getPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));

                itemViews.add(new CartItemView(
                        variant.getId(),
                        productId,
                        productName,
                        variant.getPresentationName(),
                        variant.getSku(),
                        imageUrl,
                        item.getQuantity(),
                        variant.getPriceAmount(),
                        variant.getPriceCurrency(),
                        subtotal,
                        variant.isTracksInventory(),
                        available
                ));
            } else {
                BigDecimal subtotal = item.getReferencePriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemViews.add(new CartItemView(
                        item.getVariantId(),
                        null,
                        "Producto",
                        "Presentación",
                        null,
                        null,
                        item.getQuantity(),
                        item.getReferencePriceAmount(),
                        item.getReferencePriceCurrency(),
                        subtotal,
                        false,
                        false
                ));
            }
        }

        BigDecimal totalAmount = itemViews.stream()
                .map(CartItemView::subtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemViews.stream()
                .mapToInt(CartItemView::quantity)
                .sum();

        return new CartView(
                cart.getId(),
                cart.getUserId(),
                cart.getStoreId(),
                itemViews,
                totalItems,
                totalAmount,
                currency,
                cart.getUpdatedAt()
        );
    }
}
