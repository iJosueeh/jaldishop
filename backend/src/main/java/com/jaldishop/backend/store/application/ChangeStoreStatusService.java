package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeStoreStatusService {

    private final StoreRepository storeRepository;

    public ChangeStoreStatusService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(ChangeStoreStatusCommand command) {
        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + command.storeId()));

        if (command.targetStatus() == StoreStatus.SUSPENDED) {
            store.suspend();
        } else if (command.targetStatus() == StoreStatus.ACTIVE) {
            store.activate();
        } else {
            throw new IllegalArgumentException("Estado no soportado para cambio de estado de tienda: " + command.targetStatus());
        }

        return storeRepository.save(store);
    }
}
