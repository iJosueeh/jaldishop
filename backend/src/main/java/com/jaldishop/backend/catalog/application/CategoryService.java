package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category createCategory(CreateCategoryCommand command) {
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }

        String trimmedName = command.name().trim();
        if (categoryRepository.existsByStoreIdAndNameIgnoreCase(command.storeId(), trimmedName)) {
            throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con este nombre en la tienda");
        }

        Category category = Category.create(
                command.storeId(),
                trimmedName,
                command.description()
        );

        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByStore(UUID storeId) {
        return categoryRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public Category getCategoryByIdAndStore(UUID categoryId, UUID storeId) {
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));
    }

    @Transactional
    public Category updateCategory(UpdateCategoryCommand command) {
        Category category = getCategoryByIdAndStore(command.categoryId(), command.storeId());

        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");
        }

        String trimmedName = command.name().trim();
        if (categoryRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(command.storeId(), trimmedName, command.categoryId())) {
            throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con este nombre en la tienda");
        }

        category.update(trimmedName, command.description());

        if (command.status() != null && !command.status().isBlank()) {
            CategoryStatus newStatus = CategoryStatus.valueOf(command.status().trim().toUpperCase());
            if (newStatus == CategoryStatus.ACTIVE) {
                category.activate();
            } else {
                category.deactivate();
            }
        }

        return categoryRepository.save(category);
    }
}