package com.jaldishop.backend.store.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoreCustomerTest {

    @Test
    void shouldCreateStoreCustomerSuccessfully() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        StoreCustomer customer = StoreCustomer.create(storeId, userId);

        assertNotNull(customer);
        assertEquals(storeId, customer.getStoreId());
        assertEquals(userId, customer.getUserId());
        assertNotNull(customer.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        UUID validId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new StoreCustomer(null, validId, Instant.now()));
        assertThrows(NullPointerException.class, () -> new StoreCustomer(validId, null, Instant.now()));
    }

    @Test
    void shouldVerifyEqualityBasedOnStoreIdAndUserId() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        StoreCustomer c1 = new StoreCustomer(storeId, userId, Instant.now());
        StoreCustomer c2 = new StoreCustomer(storeId, userId, Instant.now().minusSeconds(100));
        StoreCustomer c3 = new StoreCustomer(UUID.randomUUID(), userId, Instant.now());

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }
}
