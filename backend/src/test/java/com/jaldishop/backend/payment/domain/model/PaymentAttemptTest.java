package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentMethod;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAttemptTest {

    @Test
    @DisplayName("create inicializa el intento en estado CREATED y valida datos obligatorios")
    void shouldCreatePaymentAttempt() {
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "mp-pref-key-001";

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId,
                1,
                PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD,
                idempotencyKey
        );

        assertThat(attempt.getId()).isNotNull();
        assertThat(attempt.getPaymentId()).isEqualTo(paymentId);
        assertThat(attempt.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt.getProvider()).isEqualTo(PaymentProvider.MERCADO_PAGO);
        assertThat(attempt.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(attempt.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CREATED);
        assertThat(attempt.getPreferenceId()).isNull();
        assertThat(attempt.getInitPoint()).isNull();
    }

    @Test
    @DisplayName("create rechaza attemptNumber <= 0 o campos nulos")
    void shouldRejectInvalidAttemptNumber() {
        UUID paymentId = UUID.randomUUID();

        assertThatThrownBy(() -> PaymentAttempt.create(paymentId, 0, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "key"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PaymentAttempt.create(null, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "key"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("recordPreference almacena preference_id e init_point de Checkout Pro")
    void shouldRecordPreference() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idem-1"
        );

        attempt.recordPreference("pref-123456", "https://mercadopago.com/checkout/123456");

        assertThat(attempt.getPreferenceId()).isEqualTo("pref-123456");
        assertThat(attempt.getProviderOrderId()).isNull();
        assertThat(attempt.getInitPoint()).isEqualTo("https://mercadopago.com/checkout/123456");
    }

    @Test
    @DisplayName("recordOrder almacena provider_order_id e init_point de Orders API sin alterar preferenceId")
    void shouldRecordOrder() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idem-order-1"
        );

        attempt.recordOrder("ORD01J67CQQH5904WDBVZEM4JMEP3", "https://mercadopago.com.pe/checkout/v1/redirect?order_id=ORD01J67CQQ");

        assertThat(attempt.getProviderOrderId()).isEqualTo("ORD01J67CQQH5904WDBVZEM4JMEP3");
        assertThat(attempt.getPreferenceId()).isNull();
        assertThat(attempt.getProviderPaymentId()).isNull();
        assertThat(attempt.getInitPoint()).isEqualTo("https://mercadopago.com.pe/checkout/v1/redirect?order_id=ORD01J67CQQ");
    }

    @Test
    @DisplayName("recordOrder rechaza provider_order_id nulo o en blanco")
    void shouldRejectInvalidProviderOrderId() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idem-order-2"
        );

        assertThatThrownBy(() -> attempt.recordOrder(null, "https://mp.com/checkout"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> attempt.recordOrder("   ", "https://mp.com/checkout"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rehydrate preserva compatibilidad histórica cuando providerOrderId es nulo")
    void shouldRehydrateLegacyAttemptWithoutProviderOrderId() {
        UUID id = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.now();

        // Sobrecarga histórica sin providerOrderId
        PaymentAttempt legacy = PaymentAttempt.rehydrate(
                id, paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "key-legacy",
                "pref-old-999", "https://mp.com/pref/999", "PAY-LEGACY-01", "approved", "accredited",
                PaymentAttemptStatus.APPROVED, null, null, now, now, now
        );

        assertThat(legacy.getId()).isEqualTo(id);
        assertThat(legacy.getPreferenceId()).isEqualTo("pref-old-999");
        assertThat(legacy.getProviderOrderId()).isNull();
        assertThat(legacy.getProviderPaymentId()).isEqualTo("PAY-LEGACY-01");

        // Sobrecarga moderna con providerOrderId
        PaymentAttempt modern = PaymentAttempt.rehydrate(
                id, paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "key-modern",
                null, "ORD-MODERN-888", "https://mp.com/order/888", "PAY-MODERN-02", "approved", "accredited",
                PaymentAttemptStatus.APPROVED, null, null, now, now, now
        );

        assertThat(modern.getPreferenceId()).isNull();
        assertThat(modern.getProviderOrderId()).isEqualTo("ORD-MODERN-888");
        assertThat(modern.getProviderPaymentId()).isEqualTo("PAY-MODERN-02");
    }

    @Test
    @DisplayName("markApproved actualiza estado, providerPaymentId y método resuelto")
    void shouldMarkApproved() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idem-1"
        );

        Instant completedAt = Instant.now();
        attempt.markApproved("MP-PAY-777", "approved", "accredited", PaymentMethod.YAPE, completedAt);

        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(attempt.getProviderPaymentId()).isEqualTo("MP-PAY-777");
        assertThat(attempt.getProviderStatus()).isEqualTo("approved");
        assertThat(attempt.getProviderStatusDetail()).isEqualTo("accredited");
        assertThat(attempt.getPaymentMethod()).isEqualTo(PaymentMethod.YAPE);
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("markRejected y markError registran detalles de error de la pasarela")
    void shouldMarkRejectedAndError() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idem-1"
        );

        attempt.markRejected("MP-PAY-888", "rejected", "cc_rejected_insufficient_amount", "INSUFFICIENT_FUNDS", "Fondos insuficientes", Instant.now());
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.REJECTED);
        assertThat(attempt.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");

        attempt.markError("GATEWAY_TIMEOUT", "Timeout al invocar proveedor");
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.ERROR);
        assertThat(attempt.getErrorCode()).isEqualTo("GATEWAY_TIMEOUT");
    }

}
