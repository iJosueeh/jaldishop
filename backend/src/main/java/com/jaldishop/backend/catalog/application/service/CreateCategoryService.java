package com.jaldishop.backend.catalog.application.service;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.domain.repository.CategoryRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCategoryService {

    private final CategoryRepository categoryRepository;

    public CreateCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category execute(CreateCategoryCommand command) {
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
}
