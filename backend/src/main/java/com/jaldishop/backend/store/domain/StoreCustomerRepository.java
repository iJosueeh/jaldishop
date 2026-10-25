package com.jaldishop.backend.store.domain;

import java.util.List;
import java.util.UUID;

public interface StoreCustomerRepository {
    void registerCustomer(UUID storeId, UUID userId);
    boolean existsByStoreIdAndUserId(UUID storeId, UUID userId);
    List<StoreCustomerSummary> findCustomersByStoreId(UUID storeId, String query);
}
