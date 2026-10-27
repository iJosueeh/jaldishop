package com.jaldishop.backend.favorite.web.controller;

import com.jaldishop.backend.favorite.application.AddFavoriteProductService;
import com.jaldishop.backend.favorite.application.CheckFavoriteProductService;
import com.jaldishop.backend.favorite.application.GetCustomerFavoriteProductsService;
import com.jaldishop.backend.favorite.application.RemoveFavoriteProductService;
import com.jaldishop.backend.favorite.web.dto.FavoriteProductResponse;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/favorites")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerFavoriteController {

    private final AddFavoriteProductService addFavoriteProductService;
    private final RemoveFavoriteProductService removeFavoriteProductService;
    private final CheckFavoriteProductService checkFavoriteProductService;
    private final GetCustomerFavoriteProductsService getCustomerFavoriteProductsService;

    public CustomerFavoriteController(AddFavoriteProductService addFavoriteProductService,
                                      RemoveFavoriteProductService removeFavoriteProductService,
                                      CheckFavoriteProductService checkFavoriteProductService,
                                      GetCustomerFavoriteProductsService getCustomerFavoriteProductsService) {
        this.addFavoriteProductService = addFavoriteProductService;
        this.removeFavoriteProductService = removeFavoriteProductService;
        this.checkFavoriteProductService = checkFavoriteProductService;
        this.getCustomerFavoriteProductsService = getCustomerFavoriteProductsService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<FavoriteProductResponse>> getFavoriteProducts(
            @AuthenticationPrincipal JwtPrincipal principal) {
        List<FavoriteProductResponse> favorites = getCustomerFavoriteProductsService.execute(principal.userId());
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<FavoriteStatusResponse> addFavoriteProduct(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        FavoriteStatusResponse response = addFavoriteProductService.execute(principal.userId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<FavoriteStatusResponse> removeFavoriteProduct(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        FavoriteStatusResponse response = removeFavoriteProductService.execute(principal.userId(), productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}/status")
    public ResponseEntity<FavoriteStatusResponse> checkFavoriteProductStatus(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        FavoriteStatusResponse response = checkFavoriteProductService.execute(principal.userId(), productId);
        return ResponseEntity.ok(response);
    }
}
