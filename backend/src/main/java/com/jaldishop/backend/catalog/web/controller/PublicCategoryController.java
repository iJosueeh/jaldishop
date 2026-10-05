package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.GetPublicCategoriesService;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.mapper.CategoryResponseMapper;
import com.jaldishop.backend.store.application.GetStoreBySlugService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
public class PublicCategoryController {
    private final GetPublicCategoriesService service;
    private final GetStoreBySlugService getStoreBySlugService;
    private final CategoryResponseMapper mapper;

    public PublicCategoryController(GetPublicCategoriesService service, GetStoreBySlugService getStoreBySlugService, CategoryResponseMapper mapper) {
        this.service = service;
        this.getStoreBySlugService = getStoreBySlugService;
        this.mapper = mapper;
    }

    @GetMapping("/{storeId}/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(@PathVariable UUID storeId) {
        return ResponseEntity.ok(service.execute(storeId).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/slug/{slug}/categories")
    public ResponseEntity<List<CategoryResponse>> getCategoriesBySlug(@PathVariable String slug) {
        return getCategories(getStoreBySlugService.execute(slug).getId());
    }
}
