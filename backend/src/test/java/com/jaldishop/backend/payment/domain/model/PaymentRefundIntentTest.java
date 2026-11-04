package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentRefundIntent — Pruebas unitarias de dominio")
class PaymentRefundIntentTest {

    @Test
    @DisplayName("Debe crear intención de reembolso válida con clave de idempotencia estable y lease inicial")
    void shouldCreateValidRefundIntent() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId,
                "mp-pay-12345",
                BigDecimal.valueOf(45.00),
                "PEN",
                "refund-" + paymentId,
                workerToken,
                Duration.ofMinutes(10),
                now
        );

        assertNotNull(intent.getId());
        assertEquals(paymentId, intent.getPaymentId());
        assertEquals("mp-pay-12345", intent.getProviderPaymentId());
        assertEquals(BigDecimal.valueOf(45.00), intent.getAmount());
        assertEquals("PEN", intent.getCurrency());
        assertEquals("refund-" + paymentId, intent.getIdempotencyKey());
        assertEquals(RefundIntentStatus.PROCESSING, intent.getStatus());
        assertEquals(workerToken, intent.getClaimToken());
        assertTrue(intent.getLeaseExpiresAt().isAfter(now));
    }

    @Test
    @DisplayName("No debe permitir reclamo si el lease está activo para otro worker")
    void shouldNotAllowClaimIfLeaseIsActive() {
        UUID paymentId = UUID.randomUUID();
        UUID worker1 = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId,
                "mp-pay-12345",
                BigDecimal.valueOf(45.00),
                "PEN",
                "refund-" + paymentId,
                worker1,
                Duration.ofMinutes(10),
                now
        );

        // A los 2 minutos, el lease sigue activo
        assertFalse(intent.canBeClaimed(now.plus(Duration.ofMinutes(2))));
        assertTrue(intent.isClaimOwnedBy(worker1, now.plus(Duration.ofMinutes(2))));
    }

    @Test
    @DisplayName("Debe permitir reclamo si el lease ha expirado")
    void shouldAllowClaimIfLeaseExpired() {
        UUID paymentId = UUID.randomUUID();
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId,
                "mp-pay-12345",
                BigDecimal.valueOf(45.00),
                "PEN",
                "refund-" + paymentId,
                worker1,
                Duration.ofMinutes(10),
                now
        );

        // A los 11 minutos, el lease expiró
        Instant expiredAt = now.plus(Duration.ofMinutes(11));
        assertTrue(intent.canBeClaimed(expiredAt));

        // Worker 2 toma el claim
        intent.acquireClaim(worker2, Duration.ofMinutes(10), expiredAt);
        assertEquals(worker2, intent.getClaimToken());
        assertTrue(intent.isClaimOwnedBy(worker2, expiredAt));
        assertFalse(intent.isClaimOwnedBy(worker1, expiredAt));
    }

    @Test
    @DisplayName("Debe completar intención con id de reembolso de proveedor")
    void shouldCompleteRefundIntent() {
        UUID paymentId = UUID.randomUUID();
        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId, "mp-pay-12345", BigDecimal.valueOf(45.00), "PEN",
                "refund-" + paymentId, UUID.randomUUID(), Duration.ofMinutes(10), Instant.now()
        );

        Instant completedAt = Instant.now();
        intent.complete("ref-999888", completedAt);

        assertEquals(RefundIntentStatus.COMPLETED, intent.getStatus());
        assertEquals("ref-999888", intent.getProviderRefundId());
        assertEquals(completedAt, intent.getCompletedAt());
        assertFalse(intent.canBeClaimed(completedAt.plus(Duration.ofHours(1))));
    }

    @Test
    @DisplayName("Debe marcar intención como FAILED ante rechazo definitivo")
    void shouldFailRefundIntent() {
        UUID paymentId = UUID.randomUUID();
        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId, "mp-pay-12345", BigDecimal.valueOf(45.00), "PEN",
                "refund-" + paymentId, UUID.randomUUID(), Duration.ofMinutes(10), Instant.now()
        );

        Instant failedAt = Instant.now();
        intent.fail("Error 2024: Payment too old", failedAt);

        assertEquals(RefundIntentStatus.FAILED, intent.getStatus());
        assertEquals("Error 2024: Payment too old", intent.getFailureReason());
        assertFalse(intent.canBeClaimed(failedAt.plus(Duration.ofHours(1))));
    }

    @Test
    @DisplayName("Debe marcar intención como UNKNOWN ante timeout y permitir re-reclamo posterior")
    void shouldMarkUnknownAndAllowReclaim() {
        UUID paymentId = UUID.randomUUID();
        PaymentRefundIntent intent = PaymentRefundIntent.create(
                paymentId, "mp-pay-12345", BigDecimal.valueOf(45.00), "PEN",
                "refund-" + paymentId, UUID.randomUUID(), Duration.ofMinutes(10), Instant.now()
        );

        intent.markUnknown("SocketTimeoutException", Instant.now());
        assertEquals(RefundIntentStatus.UNKNOWN, intent.getStatus());
        assertTrue(intent.canBeClaimed(Instant.now()));
    }
}
