package com.jaldishop.backend.catalog.application.service;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.domain.enums.CategoryStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPublicCategoriesService {
    private final GetCategoriesService getCategoriesService;
    private final StoreRepository storeRepository;

    public GetPublicCategoriesService(GetCategoriesService getCategoriesService, StoreRepository storeRepository) {
        this.getCategoriesService = getCategoriesService;
        this.storeRepository = storeRepository;
    }

    public List<Category> execute(UUID storeId) {
        var store = storeRepository.findById(storeId)
                .filter(value -> value.getStatus() == StoreStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("La tienda no se encuentra activa o disponible."));
        return getCategoriesService.execute(store.getId()).stream()
                .filter(category -> category.getStatus() == CategoryStatus.ACTIVE)
                .filter(category -> storeId.equals(category.getStoreId()))
                .toList();
    }
}
