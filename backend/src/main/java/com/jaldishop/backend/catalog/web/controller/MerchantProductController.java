package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductCommand;
import com.jaldishop.backend.catalog.application.ProductService;
import com.jaldishop.backend.catalog.application.UpdateProductCommand;
import com.jaldishop.backend.catalog.web.dto.CreateProductRequest;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/products")
public class MerchantProductController {

    private final ProductService productService;

    public MerchantProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        var command = new CreateProductCommand(
                storeId,
                request.categoryId(),
                request.name(),
                request.slug(),
                request.description(),
                request.imageUrl()
        );
        var created = productService.createProduct(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromDomain(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId
    ) {
        List<ProductResponse> products;
        if (categoryId != null) {
            products = productService.getProductsByStoreAndCategory(storeId, categoryId).stream()
                    .map(ProductResponse::fromDomain)
                    .collect(Collectors.toList());
        } else {
            products = productService.getProductsByStore(storeId).stream()
                    .map(ProductResponse::fromDomain)
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        var product = productService.getProductByIdAndStore(productId, storeId);
        return ResponseEntity.ok(ProductResponse.fromDomain(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        var command = new UpdateProductCommand(
                productId,
                storeId,
                request.categoryId(),
                request.name(),
                request.slug(),
                request.description(),
                request.imageUrl(),
                request.status()
        );
        var updated = productService.updateProduct(command);
        return ResponseEntity.ok(ProductResponse.fromDomain(updated));
    }
}