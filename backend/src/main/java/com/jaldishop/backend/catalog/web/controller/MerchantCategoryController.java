package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CategoryService;
import com.jaldishop.backend.catalog.application.CreateCategoryCommand;
import com.jaldishop.backend.catalog.application.UpdateCategoryCommand;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.dto.CreateCategoryRequest;
import com.jaldishop.backend.catalog.web.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/categories")
public class MerchantCategoryController {

    private final CategoryService categoryService;

    public MerchantCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        var command = new CreateCategoryCommand(
                storeId,
                request.name(),
                request.description()
        );
        var created = categoryService.createCategory(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.fromDomain(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(@PathVariable UUID storeId) {
        var list = categoryService.getCategoriesByStore(storeId).stream()
                .map(CategoryResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId
    ) {
        var category = categoryService.getCategoryByIdAndStore(categoryId, storeId);
        return ResponseEntity.ok(CategoryResponse.fromDomain(category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
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