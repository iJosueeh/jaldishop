package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.store.application.GetStoreCategoriesService;
import com.jaldishop.backend.store.web.dto.StoreCategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/store-categories")
public class StoreCategoryController {

    private final GetStoreCategoriesService getStoreCategoriesService;

    public StoreCategoryController(GetStoreCategoriesService getStoreCategoriesService) {
        this.getStoreCategoriesService = getStoreCategoriesService;
    }

    @GetMapping
    public ResponseEntity<List<StoreCategoryResponse>> getAllActiveCategories() {
        List<StoreCategoryResponse> categories = getStoreCategoriesService.execute().stream()
                .map(StoreCategoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(categories);
    }
}
