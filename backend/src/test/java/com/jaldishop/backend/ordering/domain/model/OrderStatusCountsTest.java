package com.jaldishop.backend.ordering.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusCountsTest {

    @Test
    @DisplayName("of(): calcula 'all' como la suma de todos los estados válidos, incluyendo CANCELLED (RN-COUNT-02)")
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
    @DisplayName("activeOrdersCount(): calcula exclusivamente la suma de CONFIRMED + IN_PREPARATION + READY (RN-COUNT-03)")
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
    @DisplayName("empty(): inicializa todos los contadores en 0")
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

    @Test
    @DisplayName("Constructor canónico: permite instanciación válida cuando 'all' equivale exactamente a la suma de estados")
    void shouldAllowInstantiationWhenAllEqualsSumOfStatuses() {
        OrderStatusCounts counts = new OrderStatusCounts(15, 3, 2, 1, 4, 3, 2);

        assertThat(counts.all()).isEqualTo(15);
        assertThat(counts.confirmed()).isEqualTo(3);
        assertThat(counts.inPreparation()).isEqualTo(2);
        assertThat(counts.ready()).isEqualTo(1);
        assertThat(counts.outForDelivery()).isEqualTo(4);
        assertThat(counts.completed()).isEqualTo(3);
        assertThat(counts.cancelled()).isEqualTo(2);
    }

    @Test
    @DisplayName("Constructor canónico: lanza IllegalArgumentException cuando 'all' difiere de la suma de estados")
    void shouldThrowExceptionWhenAllDoesNotEqualSumOfStatuses() {
        assertThatThrownBy(() -> new OrderStatusCounts(20, 3, 2, 1, 4, 3, 2)) // suma real = 15
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invariante violada: 'all' (20) debe ser exactamente la suma de los seis estados (15).");
    }

    @ParameterizedTest(name = "Valores: all={0}, confirmed={1}, inPrep={2}, ready={3}, outForDelivery={4}, completed={5}, cancelled={6}")
    @CsvSource({
            "-1,  0, 0, 0, 0, 0, 0",
            " 0, -1, 0, 0, 0, 0, 0",
            " 0,  0,-1, 0, 0, 0, 0",
            " 0,  0, 0,-1, 0, 0, 0",
            " 0,  0, 0, 0,-1, 0, 0",
            " 0,  0, 0, 0, 0,-1, 0",
            " 0,  0, 0, 0, 0, 0,-1"
    })
    @DisplayName("Constructor canónico: lanza IllegalArgumentException cuando cualquier contador es negativo")
    void shouldThrowExceptionWhenAnyCountIsNegative(
            long all,
            long confirmed,
            long inPreparation,
            long ready,
            long outForDelivery,
            long completed,
            long cancelled
    ) {
        assertThatThrownBy(() -> new OrderStatusCounts(all, confirmed, inPreparation, ready, outForDelivery, completed, cancelled))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Los contadores de estado de pedidos no pueden ser negativos.");
    }
}
