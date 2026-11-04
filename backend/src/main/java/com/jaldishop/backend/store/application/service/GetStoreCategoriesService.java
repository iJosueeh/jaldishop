package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.domain.enums.StoreCategoryStatus;

import com.jaldishop.backend.store.domain.model.StoreCategory;
import com.jaldishop.backend.store.domain.repository.StoreCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetStoreCategoriesService {

    private final StoreCategoryRepository repository;

    public GetStoreCategoriesService(StoreCategoryRepository repository) {
        this.repository = repository;
    }

    public List<StoreCategory> execute() {
        return repository.findAll();
    }
}
