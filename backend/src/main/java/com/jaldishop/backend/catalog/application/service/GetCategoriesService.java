package com.jaldishop.backend.catalog.application.service;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.domain.repository.CategoryRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCategoriesService {

    private final CategoryRepository categoryRepository;

    public GetCategoriesService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> execute(UUID storeId) {
        return categoryRepository.findByStoreId(storeId);
    }

    public Category execute(UUID categoryId, UUID storeId) {
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));
    }
}
