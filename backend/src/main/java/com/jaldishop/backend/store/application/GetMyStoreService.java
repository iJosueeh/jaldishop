package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetMyStoreService {

    private final StoreRepository storeRepository;

    public GetMyStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(UUID merchantUserId) {
        return storeRepository.findByMerchantUserId(merchantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda para este comerciante."));
    }

}
