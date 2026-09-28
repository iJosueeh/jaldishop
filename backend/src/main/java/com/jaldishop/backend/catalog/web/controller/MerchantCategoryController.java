package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateCategoryCommand;
import com.jaldishop.backend.catalog.application.CreateCategoryService;
import com.jaldishop.backend.catalog.application.GetCategoriesService;
import com.jaldishop.backend.catalog.application.UpdateCategoryCommand;
import com.jaldishop.backend.catalog.application.UpdateCategoryService;
import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.dto.CreateCategoryRequest;
import com.jaldishop.backend.catalog.web.dto.UpdateCategoryRequest;
import com.jaldishop.backend.catalog.web.mapper.CategoryResponseMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/categories")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantCategoryController {

    private final CreateCategoryService createCategoryService;
    private final UpdateCategoryService updateCategoryService;
    private final GetCategoriesService getCategoriesService;
    private final StoreContextService storeContextService;
    private final CategoryResponseMapper responseMapper;

    public MerchantCategoryController(
            CreateCategoryService createCategoryService,
            UpdateCategoryService updateCategoryService,
            GetCategoriesService getCategoriesService,
            StoreContextService storeContextService,
            CategoryResponseMapper responseMapper
    ) {
        this.createCategoryService = createCategoryService;
        this.updateCategoryService = updateCategoryService;
        this.getCategoriesService = getCategoriesService;
        this.storeContextService = storeContextService;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        var command = new CreateCategoryCommand(
                storeId,
                request.name(),
                request.description()
        );
        Category created = createCategoryService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<Category> list = getCategoriesService.execute(storeId);
        List<CategoryResponse> response = list.stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        Category category = getCategoriesService.execute(categoryId, storeId);
        return ResponseEntity.ok(responseMapper.toResponse(category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        var command = new UpdateCategoryCommand(
                categoryId,
                storeId,
                request.name(),
                request.description(),
                request.status()
        );
        Category updated = updateCategoryService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(updated));
    }
}