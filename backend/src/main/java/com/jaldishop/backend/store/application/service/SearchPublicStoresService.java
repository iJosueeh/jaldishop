package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchPublicStoresService {

    private final StoreRepository storeRepository;

    public SearchPublicStoresService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<Store> execute(String query) {
        return storeRepository.findAllStores(query, StoreStatus.ACTIVE);
    }
}
