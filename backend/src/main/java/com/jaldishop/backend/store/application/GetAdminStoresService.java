package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminStoresService {

    private final StoreRepository storeRepository;

    public GetAdminStoresService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<Store> execute(GetAdminStoresQuery query) {
        return storeRepository.findAllStores(query.query(), query.status());
    }

    public Store execute(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + storeId));
    }
}
