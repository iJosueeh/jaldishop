package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetStoreCustomersService {

    private final StoreCustomerRepository storeCustomerRepository;

    public GetStoreCustomersService(StoreCustomerRepository storeCustomerRepository) {
        this.storeCustomerRepository = storeCustomerRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreCustomerResponse> execute(UUID storeId, String query) {
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda no puede ser nulo.");
        }
        return storeCustomerRepository.findCustomersByStoreId(storeId, query);
    }
}
