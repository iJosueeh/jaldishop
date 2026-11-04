package com.jaldishop.backend.cart.web.controller;

import com.jaldishop.backend.cart.application.command.AddItemToCartCommand;
import com.jaldishop.backend.cart.application.service.AddItemToCartService;
import com.jaldishop.backend.cart.application.result.CartView;
import com.jaldishop.backend.cart.application.command.ClearCartCommand;
import com.jaldishop.backend.cart.application.service.ClearCartService;
import com.jaldishop.backend.cart.application.query.GetCartQuery;
import com.jaldishop.backend.cart.application.service.GetCartService;
import com.jaldishop.backend.cart.application.command.RemoveCartItemCommand;
import com.jaldishop.backend.cart.application.service.RemoveCartItemService;
import com.jaldishop.backend.cart.application.command.UpdateCartItemQuantityCommand;
import com.jaldishop.backend.cart.application.service.UpdateCartItemQuantityService;
import com.jaldishop.backend.cart.web.dto.AddItemToCartRequest;
import com.jaldishop.backend.cart.web.dto.CartResponse;
import com.jaldishop.backend.cart.web.dto.UpdateCartItemRequest;
import com.jaldishop.backend.cart.web.mapper.CartResponseMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final GetCartService getCartService;
    private final AddItemToCartService addItemToCartService;
    private final UpdateCartItemQuantityService updateCartItemQuantityService;
    private final RemoveCartItemService removeCartItemService;
    private final ClearCartService clearCartService;
    private final CartResponseMapper responseMapper;

    public CartController(
            GetCartService getCartService,
            AddItemToCartService addItemToCartService,
            UpdateCartItemQuantityService updateCartItemQuantityService,
            RemoveCartItemService removeCartItemService,
            ClearCartService clearCartService,
            CartResponseMapper responseMapper
    ) {
        this.getCartService = getCartService;
        this.addItemToCartService = addItemToCartService;
        this.updateCartItemQuantityService = updateCartItemQuantityService;
        this.removeCartItemService = removeCartItemService;
        this.clearCartService = clearCartService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    ) {
        CartView cartView = getCartService.execute(new GetCartQuery(principal.userId(), storeId));
        return ResponseEntity.ok(responseMapper.toResponse(cartView));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AddItemToCartRequest request
    ) {
        var command = new AddItemToCartCommand(
                principal.userId(),
                request.storeId(),
                request.variantId(),
                request.quantity()
        );
        CartView cartView = addItemToCartService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(cartView));
    }

    @PutMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        var command = new UpdateCartItemQuantityCommand(
                principal.userId(),
                request.storeId(),
                variantId,
                request.quantity()
        );
        CartView cartView = updateCartItemQuantityService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(cartView));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @RequestParam UUID storeId
    ) {
        var command = new RemoveCartItemCommand(
                principal.userId(),
                storeId,
                variantId
        );
        CartView cartView = removeCartItemService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(cartView));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    ) {
        var command = new ClearCartCommand(
                principal.userId(),
                storeId
        );
        CartView cartView = clearCartService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(cartView));
    }
}
