package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductCommand;
import com.jaldishop.backend.catalog.application.CreateProductService;
import com.jaldishop.backend.catalog.application.GetProductsQuery;
import com.jaldishop.backend.catalog.application.GetProductsService;
import com.jaldishop.backend.catalog.application.UpdateProductCommand;
import com.jaldishop.backend.catalog.application.UpdateProductService;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.web.dto.CreateProductRequest;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.dto.UpdateProductRequest;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/products")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantProductController {

    private final CreateProductService createProductService;
    private final UpdateProductService updateProductService;
    private final GetProductsService getProductsService;
    private final StoreContextService storeContextService;
    private final ProductResponseMapper responseMapper;
    private final ProductVariantRepository variantRepository;

    public MerchantProductController(
            CreateProductService createProductService,
            UpdateProductService updateProductService,
            GetProductsService getProductsService,
            StoreContextService storeContextService,
            ProductResponseMapper responseMapper,
            ProductVariantRepository variantRepository
    ) {
        this.createProductService = createProductService;
        this.updateProductService = updateProductService;
        this.getProductsService = getProductsService;
        this.storeContextService = storeContextService;
        this.responseMapper = responseMapper;
        this.variantRepository = variantRepository;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        var command = new CreateProductCommand(
                storeId,
                request.categoryId(),
                request.name(),
                request.slug(),
                request.description(),
                request.imageUrl()
        );
        Product created = createProductService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<Product> products = getProductsService.execute(new GetProductsQuery(storeId, categoryId));

        if (variantRepository == null) {
            List<ProductResponse> response = products.stream()
                    .map(responseMapper::toResponse)
                    .toList();
            return ResponseEntity.ok(response);
        }

        List<ProductVariant> allVariants = variantRepository.findByStoreId(storeId);
        Map<UUID, List<ProductVariant>> variantsByProductId = allVariants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        List<ProductResponse> response = products.stream()
                .map(product -> {
                    List<ProductVariant> vars = variantsByProductId.get(product.getId());
                    return (vars != null && !vars.isEmpty())
                            ? responseMapper.toResponse(product, vars)
                            : responseMapper.toResponse(product);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        Product product = getProductsService.execute(productId, storeId);
        List<ProductVariant> variants = variantRepository != null
                ? variantRepository.findByProductId(productId)
                : null;
        if (variants != null && !variants.isEmpty()) {
            return ResponseEntity.ok(responseMapper.toResponse(product, variants));
        }
        return ResponseEntity.ok(responseMapper.toResponse(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
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
        Product updated = updateProductService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(updated));
    }
}