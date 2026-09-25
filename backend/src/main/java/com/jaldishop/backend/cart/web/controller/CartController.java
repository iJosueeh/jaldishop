package com.jaldishop.backend.cart.web.controller;

import com.jaldishop.backend.cart.application.AddItemToCartCommand;
import com.jaldishop.backend.cart.application.CartService;
import com.jaldishop.backend.cart.application.CartView;
import com.jaldishop.backend.cart.application.UpdateCartItemQuantityCommand;
import com.jaldishop.backend.cart.web.dto.AddItemToCartRequest;
import com.jaldishop.backend.cart.web.dto.CartResponse;
import com.jaldishop.backend.cart.web.dto.UpdateCartItemRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    ) {
        CartView cartView = cartService.getCart(principal.userId(), storeId);
        return ResponseEntity.ok(CartResponse.fromView(cartView));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AddItemToCartRequest request
    ) {
        var command = new AddItemToCartCommand(
                request.storeId(),
                request.variantId(),
                request.quantity()
        );
        CartView cartView = cartService.addItemToCart(principal.userId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.fromView(cartView));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        var command = new UpdateCartItemQuantityCommand(
                request.storeId(),
                variantId,
                request.quantity()
        );
        CartView cartView = cartService.updateCartItemQuantity(principal.userId(), command);
        return ResponseEntity.ok(CartResponse.fromView(cartView));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @RequestParam UUID storeId
    ) {
        CartView cartView = cartService.removeCartItem(principal.userId(), storeId, variantId);
        return ResponseEntity.ok(CartResponse.fromView(cartView));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    ) {
        CartView cartView = cartService.clearCart(principal.userId(), storeId);
        return ResponseEntity.ok(CartResponse.fromView(cartView));
    }
}
