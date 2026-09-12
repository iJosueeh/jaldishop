package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateStoreService {

    private final StoreRepository storeRepository;

    public UpdateStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(UpdateStoreCommand command) {
        Store store =  storeRepository.findByMerchantUserId(command.merchantUserId())
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda para este comerciante."));

        store.update(
                command.name(),
                command.description(),
                command.contactPhone(),
                command.address(),
                command.addressReference(),
                command.latitude(),
                command.longitude(),
                command.pickupEnabled(),
                command.deliveryEnabled(),
                command.deliveryFeeAmount(),
                command.deliveryFeeCurrency(),
                command.taxApplies(),
                command.taxRate()
        );

        return storeRepository.save(store);
    }

}
