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
        assertThat(attempt.getInitPoint()).isEqualTo("https://mercadopago.com/checkout/123456");
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
