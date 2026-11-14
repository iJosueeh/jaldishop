package com.jaldishop.backend.ordering.application.result;

import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantOrderStatusCountsResponseTest {

    @Test
    @DisplayName("from(): mapea todos los atributos correctamente desde OrderStatusCounts")
    void shouldMapAllFieldsFromOrderStatusCounts() {
        OrderStatusCounts domainCounts = OrderStatusCounts.of(12, 6, 4, 2, 20, 4);

        MerchantOrderStatusCountsResponse response = MerchantOrderStatusCountsResponse.from(domainCounts);

        assertThat(response.all()).isEqualTo(48);
        assertThat(response.confirmed()).isEqualTo(12);
        assertThat(response.inPreparation()).isEqualTo(6);
        assertThat(response.ready()).isEqualTo(4);
        assertThat(response.outForDelivery()).isEqualTo(2);
        assertThat(response.completed()).isEqualTo(20);
        assertThat(response.cancelled()).isEqualTo(4);
    }

    @Test
    @DisplayName("from(): lanza NullPointerException con mensaje explícito cuando el objeto OrderStatusCounts es nulo")
    void shouldThrowNullPointerExceptionWhenCountsIsNull() {
        assertThatThrownBy(() -> MerchantOrderStatusCountsResponse.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El objeto OrderStatusCounts no puede ser nulo.");
    }

    @Test
    @DisplayName("empty(): inicializa todos los contadores de respuesta en 0")
    void shouldInitializeAllResponseCountsToZeroOnEmpty() {
        MerchantOrderStatusCountsResponse response = MerchantOrderStatusCountsResponse.empty();

        assertThat(response.all()).isZero();
        assertThat(response.confirmed()).isZero();
        assertThat(response.inPreparation()).isZero();
        assertThat(response.ready()).isZero();
        assertThat(response.outForDelivery()).isZero();
        assertThat(response.completed()).isZero();
        assertThat(response.cancelled()).isZero();
    }
}
