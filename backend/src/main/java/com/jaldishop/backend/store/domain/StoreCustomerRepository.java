package com.jaldishop.backend.store.domain;

import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;

import java.util.List;
import java.util.UUID;

public interface StoreCustomerRepository {

    void save(StoreCustomer storeCustomer);

    boolean existsByStoreIdAndUserId(UUID storeId, UUID userId);

    List<StoreCustomerResponse> findCustomersByStoreId(UUID storeId, String query);
}
