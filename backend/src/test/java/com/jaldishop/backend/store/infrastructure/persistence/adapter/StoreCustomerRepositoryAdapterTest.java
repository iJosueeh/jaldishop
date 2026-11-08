package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.store.infrastructure.persistence.projection.StoreCustomerProjection;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreCustomerJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StoreCustomerRepositoryAdapterTest {
    @Test
    void mapsInstantBackedSpringProjectionsWithoutLosingPrecisionOrNulls() {
        var since = Instant.parse("2026-09-01T10:00:00.123456Z");
        var lastOrder = Instant.parse("2026-09-15T15:30:00.654321Z");
        var customer = UUID.randomUUID();
        var values = new HashMap<String, Object>();
        values.put("userId", customer);
        values.put("firstName", "Carlos");
        values.put("customerSince", since);
        values.put("ordersCount", 2L);
        values.put("totalSpent", new BigDecimal("85.50"));
        values.put("lastOrderAt", lastOrder);
        var factory = new SpelAwareProxyProjectionFactory();
        var withOrders = factory.createProjection(StoreCustomerProjection.class, values);
        var withoutOrders = factory.createProjection(StoreCustomerProjection.class,
                new HashMap<>(java.util.Map.of("userId", UUID.randomUUID(), "customerSince", since)));
        var repository = mock(StoreCustomerJpaRepository.class);
        var store = UUID.randomUUID();
        when(repository.findCustomersByStoreId(store, "Carlos")).thenReturn(List.of(withOrders, withoutOrders));

        var result = new StoreCustomerRepositoryAdapter(repository).findCustomersByStoreId(store, "  Carlos  ");

        assertThat(result.getFirst().userId()).isEqualTo(customer);
        assertThat(result.getFirst().customerSince()).isEqualTo(since);
        assertThat(result.getFirst().lastOrderAt()).isEqualTo(lastOrder);
        assertThat(result.getFirst().ordersCount()).isEqualTo(2L);
        assertThat(result.getFirst().totalSpentAmount()).isEqualByComparingTo("85.50");
        assertThat(result.get(1).customerSince()).isEqualTo(since);
        assertThat(result.get(1).lastOrderAt()).isNull();
        assertThat(result.get(1).ordersCount()).isZero();
        assertThat(result.get(1).totalSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
