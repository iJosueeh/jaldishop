package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
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
