package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
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
