package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CategoryService;
import com.jaldishop.backend.catalog.application.CreateCategoryCommand;
import com.jaldishop.backend.catalog.application.UpdateCategoryCommand;
import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.dto.CreateCategoryRequest;
import com.jaldishop.backend.catalog.web.dto.UpdateCategoryRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/categories")
public class MerchantCategoryController {

    private final CategoryService categoryService;
    private final StoreContextService storeContextService;

    public MerchantCategoryController(CategoryService categoryService, StoreContextService storeContextService) {
        this.categoryService = categoryService;
        this.storeContextService = storeContextService;
    }

    private void validateStoreOwnership(UUID storeId, JwtPrincipal principal) {
        UUID authenticatedStoreId = storeContextService.requireStoreId(principal);
        if (!storeId.equals(authenticatedStoreId)) {
            throw new AccessDeniedException("No tienes permisos para gestionar recursos de esta tienda.");
        }
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        validateStoreOwnership(storeId, principal);
        var command = new CreateCategoryCommand(
                storeId,
                request.name(),
                request.description()
        );
        var created = categoryService.createCategory(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.fromDomain(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId
    ) {
        validateStoreOwnership(storeId, principal);
        var list = categoryService.getCategoriesByStore(storeId).stream()
                .map(CategoryResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId
    ) {
        validateStoreOwnership(storeId, principal);
        var category = categoryService.getCategoryByIdAndStore(categoryId, storeId);
        return ResponseEntity.ok(CategoryResponse.fromDomain(category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        validateStoreOwnership(storeId, principal);
        var command = new UpdateCategoryCommand(
                categoryId,
                storeId,
                request.name(),
                request.description(),
                request.status()
        );
        var updated = categoryService.updateCategory(command);
        return ResponseEntity.ok(CategoryResponse.fromDomain(updated));
    }
}