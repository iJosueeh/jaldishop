package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.GetProductsQuery;
import com.jaldishop.backend.catalog.application.GetProductsService;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.store.application.GetStoreBySlugService;
import com.jaldishop.backend.store.domain.Store;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stores")
public class PublicProductController {

    private final GetProductsService getProductsService;
    private final ProductResponseMapper responseMapper;
    private final ProductVariantRepository variantRepository;
    private final GetStoreBySlugService getStoreBySlugService;

    public PublicProductController(
            GetProductsService getProductsService,
            ProductResponseMapper responseMapper,
            ProductVariantRepository variantRepository,
            GetStoreBySlugService getStoreBySlugService
    ) {
        this.getProductsService = getProductsService;
        this.responseMapper = responseMapper;
        this.variantRepository = variantRepository;
        this.getStoreBySlugService = getStoreBySlugService;
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
            @PathVariable String slug,
            @RequestParam(required = false) UUID categoryId
    ) {
        Store store = getStoreBySlugService.execute(slug);
        return ResponseEntity.ok(fetchActiveProducts(store.getId(), categoryId));
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

        return products.stream()
                .map(product -> {
                    List<ProductVariant> vars = variantsByProductId.get(product.getId());
                    return (vars != null && !vars.isEmpty())
                            ? responseMapper.toResponse(product, vars)
                            : responseMapper.toResponse(product);
                })
                .toList();
    }
}
