package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCustomer;
import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterStoreCustomerService {

    private final StoreCustomerRepository storeCustomerRepository;

    public RegisterStoreCustomerService(StoreCustomerRepository storeCustomerRepository) {
        this.storeCustomerRepository = storeCustomerRepository;
    }

    @Transactional
    public void execute(UUID storeId, UUID userId) {
        if (storeId == null || userId == null) {
            return;
        }

        if (!storeCustomerRepository.existsByStoreIdAndUserId(storeId, userId)) {
            StoreCustomer storeCustomer = StoreCustomer.create(storeId, userId);
            storeCustomerRepository.save(storeCustomer);
        }
    }
}
