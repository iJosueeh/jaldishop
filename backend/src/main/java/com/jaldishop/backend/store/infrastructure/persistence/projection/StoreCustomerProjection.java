package com.jaldishop.backend.store.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface StoreCustomerProjection {
    UUID getUserId();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getPhone();
    Instant getCustomerSince();
    Long getOrdersCount();
    BigDecimal getTotalSpent();
    Instant getLastOrderAt();
}
