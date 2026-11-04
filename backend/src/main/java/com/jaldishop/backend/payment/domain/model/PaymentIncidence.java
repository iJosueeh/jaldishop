package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PaymentIncidence {

    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final UUID id;
    private final UUID paymentId;
    private final IncidenceProcessType processType;
    private IncidenceCategory category;
    private String code;
    private String message;
    private int attemptsCount;
    private final int maxAttempts;
    private Instant nextRetryAt;
    private IncidenceStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public PaymentIncidence(
            UUID id,
            UUID paymentId,
            IncidenceProcessType processType,
            IncidenceCategory category,
            String code,
            String message,
            int attemptsCount,
            int maxAttempts,
            Instant nextRetryAt,
            IncidenceStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt
    ) {
        this.id = Objects.requireNonNull(id, "El ID de la incidencia es obligatorio.");
        this.paymentId = Objects.requireNonNull(paymentId, "El paymentId es obligatorio.");
        this.processType = Objects.requireNonNull(processType, "El processType es obligatorio.");
        this.category = Objects.requireNonNull(category, "La categoría de la incidencia es obligatoria.");
        this.code = Objects.requireNonNull(code, "El código de la incidencia es obligatorio.");
        this.message = sanitizeMessage(message);
        this.attemptsCount = Math.max(1, attemptsCount);
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : DEFAULT_MAX_ATTEMPTS;
        this.nextRetryAt = nextRetryAt;
        this.status = Objects.requireNonNull(status, "El estado de la incidencia es obligatorio.");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.resolvedAt = resolvedAt;
    }

    public static PaymentIncidence createNew(
            UUID paymentId,
            IncidenceProcessType processType,
            IncidenceCategory category,
            String code,
            String message,
            Duration firstBackoff,
            Instant now
    ) {
        Instant effectiveNow = now != null ? now : Instant.now();
        Instant retryAt = firstBackoff != null ? effectiveNow.plus(firstBackoff) : null;
        return new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                processType,
                category,
                code,
                message,
                1,
                DEFAULT_MAX_ATTEMPTS,
                retryAt,
                IncidenceStatus.OPEN,
                effectiveNow,
                effectiveNow,
                null
        );
    }

    public static PaymentIncidence createCommercialTerminal(
            UUID paymentId,
            IncidenceProcessType processType,
            String code,
            String message,
            Instant now
    ) {
        Instant effectiveNow = now != null ? now : Instant.now();
        return new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                processType,
                IncidenceCategory.COMMERCIAL,
                code,
                message,
                1,
                DEFAULT_MAX_ATTEMPTS,
                null,
                IncidenceStatus.RESOLVED,
                effectiveNow,
                effectiveNow,
                effectiveNow
        );
    }

    public void recordRetry(Duration nextBackoff, String newCode, String newMessage, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.attemptsCount++;
        this.updatedAt = effectiveNow;
        if (newCode != null && !newCode.isBlank()) {
            this.code = newCode;
        }
        if (newMessage != null && !newMessage.isBlank()) {
            this.message = sanitizeMessage(newMessage);
        }

        if (this.attemptsCount >= this.maxAttempts) {
            this.status = IncidenceStatus.MANUAL_REVIEW_REQUIRED;
            this.nextRetryAt = null;
        } else {
            this.status = IncidenceStatus.OPEN;
            this.nextRetryAt = nextBackoff != null ? effectiveNow.plus(nextBackoff) : null;
        }
    }

    public void markResolved(Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.status = IncidenceStatus.RESOLVED;
        this.resolvedAt = effectiveNow;
        this.updatedAt = effectiveNow;
        this.nextRetryAt = null;
    }

    public void escalateToManualReview(String reason, Instant now) {
        Instant effectiveNow = now != null ? now : Instant.now();
        this.status = IncidenceStatus.MANUAL_REVIEW_REQUIRED;
        this.updatedAt = effectiveNow;
        this.nextRetryAt = null;
        if (reason != null && !reason.isBlank()) {
            this.message = sanitizeMessage(reason);
        }
    }

    private static String sanitizeMessage(String input) {
        if (input == null || input.isBlank()) {
            return "Incidencia registrada sin detalle adicional.";
        }
        // Truncar longitud para evitar sobrecargas de logs/BD y remover eventuales credenciales/tokens
        String sanitized = input.replaceAll("(?i)(bearer\\s+[a-zA-Z0-9_.-]+|access_token=[a-zA-Z0-9_.-]+)", "[REDACTED]");
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public IncidenceProcessType getProcessType() { return processType; }
    public IncidenceCategory getCategory() { return category; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public int getAttemptsCount() { return attemptsCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public IncidenceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
