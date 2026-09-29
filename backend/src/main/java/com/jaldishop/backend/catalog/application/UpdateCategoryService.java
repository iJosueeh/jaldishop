package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCategoryService {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category execute(UpdateCategoryCommand command) {
        Category category = categoryRepository.findByIdAndStoreId(command.categoryId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));

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
