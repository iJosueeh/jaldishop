package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    @DisplayName("createPending inicializa correctamente estado PENDING y NOT_REQUIRED en reembolso con FulfillmentType")
    void shouldCreatePendingPayment() {
        UUID reservationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("125.50");

        Payment payment = Payment.createPending(
                reservationId,
                amount,
                "PEN",
                FulfillmentType.DELIVERY,
                "Carlos Silva",
                "+51987654321",
                "carlos@example.com",
                "Av. Arequipa 123",
                "Dpto 501",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793"),
                new BigDecimal("115.50"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("10.00"),
                new BigDecimal("18.00"),
                "[{\"variantId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}]"
        );

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getCapacityReservationId()).isEqualTo(reservationId);
        assertThat(payment.getAmount()).isEqualByComparingTo("125.50");
        assertThat(payment.getCurrency()).isEqualTo("PEN");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
        assertThat(payment.getRefundAmount()).isNull();
        assertThat(payment.getApprovedAt()).isNull();
        assertThat(payment.getFulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
        assertThat(payment.getCustomerName()).isEqualTo("Carlos Silva");
        assertThat(payment.getProductsSubtotalAmount()).isEqualByComparingTo("115.50");
        assertThat(payment.getDeliveryFeeAmount()).isEqualByComparingTo("10.00");
        assertThat(payment.getItemsSnapshot()).contains("variantId");
    }

    @Test
    @DisplayName("createPending rechaza monto <= 0 o moneda distinta a PEN")
    void shouldRejectInvalidAmountOrCurrency() {
        UUID reservationId = UUID.randomUUID();

        assertThatThrownBy(() -> Payment.createPending(
                reservationId, BigDecimal.ZERO, "PEN", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor a cero");

        assertThatThrownBy(() -> Payment.createPending(
                reservationId, new BigDecimal("50.00"), "USD", null, null, null, null, null, null, null, null, null, null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PEN");
    }

    @Test
    @DisplayName("markApproved cambia estado a APPROVED sin alterar el estado de reembolso")
    void shouldMarkApproved() {
        Payment payment = Payment.createPending(
                UUID.randomUUID(), new BigDecimal("80.00"), "PEN", FulfillmentType.PICKUP,
                "Ana Torres", "+51912345678", "ana@example.com", null, null, null, null,
                new BigDecimal("80.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("12.20"), "[]"
        );

        Instant approvedAt = Instant.now();
        payment.markApproved(approvedAt);

        assertThat(payment.isApproved()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("requestRefund inicia auditoría de reembolso manteniendo el resultado financiero APPROVED")
    void shouldRequestRefundAndMaintainApprovedStatus() {
        Payment payment = Payment.createPending(
                UUID.randomUUID(), new BigDecimal("100.00"), "PEN", FulfillmentType.DELIVERY,
                "Pedro Gomez", "+51999888777", "pedro@example.com", "Calle Real 456", null, null, null,
                new BigDecimal("90.00"), BigDecimal.ZERO, null, new BigDecimal("10.00"), new BigDecimal("15.25"), "[]"
        );
        payment.markApproved(Instant.now());

        payment.requestRefund(new BigDecimal("100.00"));

        // El resultado financiero original se preserva como APPROVED
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        // El resultado de compensación se encuentra en PENDING
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(payment.getRefundAmount()).isEqualByComparingTo("100.00");

        // Completar reembolso
        payment.completeRefund("REF-MP-12345", Instant.now());
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(payment.getRefundReference()).isEqualTo("REF-MP-12345");
    }

    @Test
    @DisplayName("requestRefund rechaza montos mayores al pago original")
    void shouldRejectRefundExceedingTotalAmount() {
        Payment payment = Payment.createPending(
                UUID.randomUUID(), new BigDecimal("50.00"), "PEN", FulfillmentType.PICKUP,
                "Luis Díaz", "+51900112233", "luis@example.com", null, null, null, null,
                new BigDecimal("50.00"), BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("7.62"), "[]"
        );
        payment.markApproved(Instant.now());

        assertThatThrownBy(() -> payment.requestRefund(new BigDecimal("50.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede superar");
    }

}
