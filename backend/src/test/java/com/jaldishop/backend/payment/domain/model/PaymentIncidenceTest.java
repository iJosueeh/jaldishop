package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentIncidence — Pruebas unitarias de dominio")
class PaymentIncidenceTest {

    @Test
    @DisplayName("Debe crear una incidencia nueva en estado OPEN con backoff inicial")
    void shouldCreateNewOpenIncidence() {
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T20:00:00Z");

        PaymentIncidence incidence = PaymentIncidence.createNew(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "LOCK_TIMEOUT",
                "Lock acquisition failure",
                Duration.ofMinutes(5),
                now
        );

        assertNotNull(incidence.getId());
        assertEquals(paymentId, incidence.getPaymentId());
        assertEquals(IncidenceProcessType.ORDER_CONFIRMATION, incidence.getProcessType());
        assertEquals(IncidenceCategory.TECHNICAL, incidence.getCategory());
        assertEquals("LOCK_TIMEOUT", incidence.getCode());
        assertEquals("Lock acquisition failure", incidence.getMessage());
        assertEquals(1, incidence.getAttemptsCount());
        assertEquals(5, incidence.getMaxAttempts());
        assertEquals(IncidenceStatus.OPEN, incidence.getStatus());
        assertEquals(now.plus(Duration.ofMinutes(5)), incidence.getNextRetryAt());
        assertNull(incidence.getResolvedAt());
    }

    @Test
    @DisplayName("Debe crear una incidencia comercial terminal resuelta")
    void shouldCreateCommercialTerminalIncidence() {
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T20:00:00Z");

        PaymentIncidence incidence = PaymentIncidence.createCommercialTerminal(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                "INSUFFICIENT_STOCK",
                "Inventario no disponible",
                now
        );

        assertEquals(IncidenceStatus.RESOLVED, incidence.getStatus());
        assertEquals(IncidenceCategory.COMMERCIAL, incidence.getCategory());
        assertNull(incidence.getNextRetryAt());
        assertEquals(now, incidence.getResolvedAt());
    }

    @Test
    @DisplayName("Debe incrementar intentos y calcular siguiente reintento con backoff")
    void shouldIncrementAttemptsAndApplyBackoff() {
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T20:00:00Z");

        PaymentIncidence incidence = PaymentIncidence.createNew(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "TIMEOUT",
                "Timeout 1",
                Duration.ofMinutes(5),
                now
        );

        Instant retryTime = now.plus(Duration.ofMinutes(10));
        incidence.recordRetry(Duration.ofMinutes(15), "TIMEOUT", "Timeout 2", retryTime);

        assertEquals(2, incidence.getAttemptsCount());
        assertEquals(IncidenceStatus.OPEN, incidence.getStatus());
        assertEquals(retryTime.plus(Duration.ofMinutes(15)), incidence.getNextRetryAt());
    }

    @Test
    @DisplayName("Debe escalar a MANUAL_REVIEW_REQUIRED al alcanzar el máximo de intentos")
    void shouldEscalateToManualReviewWhenMaxAttemptsReached() {
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T20:00:00Z");

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "DEADLOCK",
                "Deadlock detectado",
                4, // Ya en intento 4
                5,
                now,
                IncidenceStatus.OPEN,
                now,
                now,
                null
        );

        // Intento 5 (alcanza max_attempts)
        incidence.recordRetry(Duration.ofMinutes(30), "DEADLOCK", "Deadlock final", now.plusSeconds(60));

        assertEquals(5, incidence.getAttemptsCount());
        assertEquals(IncidenceStatus.MANUAL_REVIEW_REQUIRED, incidence.getStatus());
        assertNull(incidence.getNextRetryAt(), "No debe tener fecha de reintento si requiere revisión manual");
    }

    @Test
    @DisplayName("Debe marcar incidencia como resuelta")
    void shouldMarkAsResolved() {
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-02T20:00:00Z");

        PaymentIncidence incidence = PaymentIncidence.createNew(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "ERROR",
                "Error previo",
                Duration.ofMinutes(5),
                now
        );

        Instant resolvedAt = now.plus(Duration.ofMinutes(6));
        incidence.markResolved(resolvedAt);

        assertEquals(IncidenceStatus.RESOLVED, incidence.getStatus());
        assertEquals(resolvedAt, incidence.getResolvedAt());
        assertNull(incidence.getNextRetryAt());
    }

    @Test
    @DisplayName("Debe sanitizar tokens o credenciales en el mensaje de la incidencia")
    void shouldSanitizeSensitiveDataInMessage() {
        UUID paymentId = UUID.randomUUID();
        String sensitiveMessage = "Error connecting with Bearer secret-token-123456789 and access_token=tok_987654";

        PaymentIncidence incidence = PaymentIncidence.createNew(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "AUTH_ERROR",
                sensitiveMessage,
                Duration.ofMinutes(5),
                Instant.now()
        );

        assertFalse(incidence.getMessage().contains("secret-token-123456789"));
        assertFalse(incidence.getMessage().contains("tok_987654"));
        assertTrue(incidence.getMessage().contains("[REDACTED]"));
    }
}
