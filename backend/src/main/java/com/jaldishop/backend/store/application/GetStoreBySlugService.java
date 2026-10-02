package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class GetStoreBySlugService {

    private final StoreRepository storeRepository;

    public GetStoreBySlugService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("El slug de la tienda no puede ser nulo ni vacío.");
        }

        String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
        Store store = storeRepository.findBySlug(normalizedSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con el identificador: " + slug));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ResourceNotFoundException("La tienda no se encuentra activa o disponible.");
        }

        return store;
    }
}
