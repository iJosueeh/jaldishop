package com.jaldishop.backend.ordering.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusCountsTest {

    @Test
    @DisplayName("RN-COUNT-02: 'all' debe ser exactamente la suma de todos los estados válidos, incluyendo CANCELLED")
    void shouldComputeAllAsSumOfAllValidStatusesIncludingCancelled() {
        long confirmed = 12;
        long inPreparation = 6;
        long ready = 4;
        long outForDelivery = 2;
        long completed = 20;
        long cancelled = 4;

        OrderStatusCounts counts = OrderStatusCounts.of(
                confirmed, inPreparation, ready, outForDelivery, completed, cancelled
        );

        long expectedAll = 12 + 6 + 4 + 2 + 20 + 4; // 48
        assertThat(counts.all()).isEqualTo(expectedAll);
        assertThat(counts.confirmed()).isEqualTo(12);
        assertThat(counts.inPreparation()).isEqualTo(6);
        assertThat(counts.ready()).isEqualTo(4);
        assertThat(counts.outForDelivery()).isEqualTo(2);
        assertThat(counts.completed()).isEqualTo(20);
        assertThat(counts.cancelled()).isEqualTo(4);
    }

    @Test
    @DisplayName("RN-COUNT-03: 'activeOrdersCount' debe sumar exclusivamente CONFIRMED + IN_PREPARATION + READY")
    void shouldComputeActiveOrdersCountAsConfirmedPlusInPrepPlusReady() {
        OrderStatusCounts counts = OrderStatusCounts.of(
                10, // confirmed
                5,  // inPreparation
                3,  // ready
                8,  // outForDelivery (excluido)
                50, // completed (excluido)
                12  // cancelled (excluido)
        );

        assertThat(counts.activeOrdersCount()).isEqualTo(10 + 5 + 3); // 18
    }

    @Test
    @DisplayName("empty() debe inicializar todos los contadores en 0")
    void shouldInitializeAllCountsToZeroOnEmpty() {
        OrderStatusCounts counts = OrderStatusCounts.empty();

        assertThat(counts.all()).isZero();
        assertThat(counts.confirmed()).isZero();
        assertThat(counts.inPreparation()).isZero();
        assertThat(counts.ready()).isZero();
        assertThat(counts.outForDelivery()).isZero();
        assertThat(counts.completed()).isZero();
        assertThat(counts.cancelled()).isZero();
        assertThat(counts.activeOrdersCount()).isZero();
    }
}
