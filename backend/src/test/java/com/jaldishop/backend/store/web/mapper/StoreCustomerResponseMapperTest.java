package com.jaldishop.backend.store.web.mapper;

import com.jaldishop.backend.store.domain.StoreCustomerSummary;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoreCustomerResponseMapperTest {

    private final StoreCustomerResponseMapper mapper = new StoreCustomerResponseMapper();

    @Test
    @DisplayName("Mapea los 9 campos de StoreCustomerSummary a StoreCustomerResponse sin perder valores")
    void shouldMapAllNineFields() {
        UUID userId = UUID.randomUUID();
        Instant customerSince = Instant.parse("2026-01-10T08:30:00Z");
        Instant lastOrderAt = Instant.parse("2026-09-15T15:30:00Z");
        BigDecimal totalSpentAmount = new BigDecimal("150.00");

        StoreCustomerSummary summary = new StoreCustomerSummary(
                userId,
                "Valeria",
                "Ramos",
                "valeria@example.com",
                "+51984552109",
                customerSince,
                3L,
                totalSpentAmount,
                lastOrderAt
        );

        StoreCustomerResponse response = mapper.toResponse(summary);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.firstName()).isEqualTo("Valeria");
        assertThat(response.lastName()).isEqualTo("Ramos");
        assertThat(response.email()).isEqualTo("valeria@example.com");
        assertThat(response.phone()).isEqualTo("+51984552109");
        assertThat(response.customerSince()).isEqualTo(customerSince);
        assertThat(response.ordersCount()).isEqualTo(3L);
        assertThat(response.totalSpentAmount()).isEqualTo(totalSpentAmount);
        assertThat(response.lastOrderAt()).isEqualTo(lastOrderAt);
    }

    @Test
    @DisplayName("Preserva la semántica de nulos: customerSince y lastOrderAt opcionales viajan como null")
    void shouldPreserveNullSemantics() {
        StoreCustomerSummary summary = new StoreCustomerSummary(
                UUID.randomUUID(),
                "Sin",
                "Pedidos",
                "sinpedidos@example.com",
                null,
                null,
                0L,
                BigDecimal.ZERO,
                null
        );

        StoreCustomerResponse response = mapper.toResponse(summary);

        assertThat(response.customerSince()).isNull();
        assertThat(response.phone()).isNull();
        assertThat(response.lastOrderAt()).isNull();
        assertThat(response.ordersCount()).isZero();
        assertThat(response.totalSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
