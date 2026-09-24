package com.jaldishop.backend.store.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public interface StoreCustomerProjection {
    UUID getUserId();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getPhone();
    Timestamp getCustomerSince();
    Long getOrdersCount();
    BigDecimal getTotalSpent();
    Timestamp getLastOrderAt();
}
