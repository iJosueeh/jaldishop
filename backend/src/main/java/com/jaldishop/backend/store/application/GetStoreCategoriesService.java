package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryRepository;
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
