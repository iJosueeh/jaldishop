package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.query.GetProductsQuery;
import com.jaldishop.backend.catalog.application.service.GetProductsService;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.enums.VariantStatus;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.inventory.application.service.GetStoreInventoryService;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.store.application.service.GetStoreBySlugService;
import com.jaldishop.backend.store.domain.model.Store;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stores")
@Validated
public class PublicProductController {

    private final GetProductsService getProductsService;
    private final ProductResponseMapper responseMapper;
    private final ProductVariantRepository variantRepository;
    private final GetStoreBySlugService getStoreBySlugService;
    private final GetStoreInventoryService getStoreInventoryService;

    public PublicProductController(
            GetProductsService getProductsService,
            ProductResponseMapper responseMapper,
            ProductVariantRepository variantRepository,
            GetStoreBySlugService getStoreBySlugService
    ) {
        this(getProductsService, responseMapper, variantRepository, getStoreBySlugService, null);
    }

    @Autowired
    public PublicProductController(
            GetProductsService getProductsService,
            ProductResponseMapper responseMapper,
            ProductVariantRepository variantRepository,
            GetStoreBySlugService getStoreBySlugService,
            GetStoreInventoryService getStoreInventoryService
    ) {
        this.getProductsService = getProductsService;
        this.responseMapper = responseMapper;
        this.variantRepository = variantRepository;
        this.getStoreBySlugService = getStoreBySlugService;
        this.getStoreInventoryService = getStoreInventoryService;
    }

    @GetMapping("/{storeId}/products")
    public ResponseEntity<List<ProductResponse>> getStoreProducts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId
    ) {
        return ResponseEntity.ok(fetchActiveProducts(storeId, categoryId));
    }

    @GetMapping("/slug/{slug}/products")
    public ResponseEntity<List<ProductResponse>> getStoreProductsBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String slug,
            @RequestParam(required = false) UUID categoryId
    ) {
        Store store = getStoreBySlugService.execute(slug);
        return ResponseEntity.ok(fetchActiveProducts(store.getId(), categoryId));
    }

    @GetMapping("/slug/{storeSlug}/products/{productSlug}")
    public ResponseEntity<ProductResponse> getStoreProductBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String storeSlug,
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String productSlug
    ) {
        Store store = getStoreBySlugService.execute(storeSlug);
        Product product = getProductsService.executeBySlug(store.getId(), productSlug);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return ResponseEntity.notFound().build();
        }

        List<ProductVariant> activeVariants = variantRepository.findByProductId(product.getId()).stream()
                .filter(variant -> variant.getStatus() == VariantStatus.ACTIVE)
                .toList();

        Map<UUID, Integer> availableStockMap = loadAvailableStockMap(store.getId());
        if (availableStockMap.isEmpty()) {
            return ResponseEntity.ok(responseMapper.toResponse(product, activeVariants));
        }
        return ResponseEntity.ok(responseMapper.toResponse(product, activeVariants, availableStockMap));
    }

    private List<ProductResponse> fetchActiveProducts(UUID storeId, UUID categoryId) {
        List<Product> products = getProductsService.execute(new GetProductsQuery(storeId, categoryId))
                .stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .toList();

        if (variantRepository == null) {
            return products.stream()
                    .map(responseMapper::toResponse)
                    .toList();
        }

        List<ProductVariant> allVariants = variantRepository.findByStoreId(storeId);
        Map<UUID, List<ProductVariant>> variantsByProductId = allVariants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        Map<UUID, Integer> availableStockMap = loadAvailableStockMap(storeId);

        return products.stream()
                .map(product -> {
                    List<ProductVariant> vars = variantsByProductId.get(product.getId());
                    if (vars == null || vars.isEmpty()) {
                        return responseMapper.toResponse(product);
                    }
                    return availableStockMap.isEmpty()
                            ? responseMapper.toResponse(product, vars)
                            : responseMapper.toResponse(product, vars, availableStockMap);
                })
                .toList();
    }

    private Map<UUID, Integer> loadAvailableStockMap(UUID storeId) {
        if (getStoreInventoryService == null) {
            return Collections.emptyMap();
        }
        return getStoreInventoryService.execute(storeId).stream()
                .filter(inv -> inv.tracksInventory() && inv.availableQuantity() != null)
                .collect(Collectors.toMap(
                        InventoryResponse::variantId,
                        InventoryResponse::availableQuantity,
                        (existing, replacement) -> existing
                ));
    }
}
