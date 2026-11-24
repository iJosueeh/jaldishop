package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.domain.repository.CategoryRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ManageCategoryLifecycleService {

    private final CategoryRepository categoryRepository;

    public ManageCategoryLifecycleService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void delete(UUID categoryId, UUID storeId) {
        Category category = categoryRepository.lockByIdAndStoreIdForUpdate(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoryId.toString()));

        if (categoryRepository.hasProducts(category.getId())) {
            throw new ResourceConflictException(
                    "CATEGORY_HAS_PRODUCTS",
                    "Esta categoría tiene productos asociados y no puede eliminarse permanentemente. Puedes desactivarla o reasignar sus productos primero.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", category.getStatus().name(),
                            "blockingReason", "CATEGORY_HAS_PRODUCTS"
                    )
            );
        }

        categoryRepository.deleteByIdAndStoreId(category.getId(), storeId);
    }

    public Category updateStatus(UUID categoryId, UUID storeId, String status) {
        Category category = categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoryId.toString()));

        if ("ACTIVE".equalsIgnoreCase(status)) {
            category.activate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            category.deactivate();
        } else {
            throw new IllegalArgumentException("Estado de categoría no válido: " + status);
        }

        return categoryRepository.save(category);
    }
}
