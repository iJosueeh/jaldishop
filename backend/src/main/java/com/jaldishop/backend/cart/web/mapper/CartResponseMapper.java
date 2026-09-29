package com.jaldishop.backend.cart.web.mapper;

import com.jaldishop.backend.cart.application.CartItemView;
import com.jaldishop.backend.cart.application.CartView;
import com.jaldishop.backend.cart.web.dto.CartItemResponse;
import com.jaldishop.backend.cart.web.dto.CartResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartResponseMapper {

    public CartResponse toResponse(CartView view) {
        if (view == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = view.items() != null
                ? view.items().stream().map(this::toItemResponse).toList()
                : List.of();

        return new CartResponse(
                view.id(),
                view.userId(),
                view.storeId(),
                itemResponses,
                view.totalItems(),
                view.totalAmount(),
                view.currency(),
                view.updatedAt()
        );
    }

    public CartItemResponse toItemResponse(CartItemView view) {
        if (view == null) {
            return null;
        }

        return new CartItemResponse(
                view.variantId(),
                view.productId(),
                view.productName(),
                view.presentationName(),
                view.sku(),
                view.imageUrl(),
                view.quantity(),
                view.unitPriceAmount(),
                view.unitPriceCurrency(),
                view.subtotalAmount(),
                view.tracksInventory(),
                view.available()
        );
    }
}
