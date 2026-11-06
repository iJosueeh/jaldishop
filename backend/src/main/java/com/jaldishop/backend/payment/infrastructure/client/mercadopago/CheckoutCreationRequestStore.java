package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class CheckoutCreationRequestStore {
    private final JdbcTemplate jdbc;

    public CheckoutCreationRequestStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] saveBeforeSend(UUID attemptId, String mode, String key, byte[] body) {
        if (attemptId == null || key == null || key.isBlank()
                || !("PREFERENCES".equals(mode) || "ORDERS".equals(mode))) {
            throw new IllegalArgumentException("Contexto de solicitud de checkout inválido");
        }
        int inserted = jdbc.update("""
                INSERT INTO checkout_creation_requests (payment_attempt_id, checkout_mode, idempotency_key, format_version, request_body)
                SELECT id, checkout_mode, idempotency_key, 1, ? FROM payment_attempts
                WHERE id = ? AND checkout_mode = ? AND idempotency_key = ?
                ON CONFLICT (payment_attempt_id) DO NOTHING
                """, new String(body, StandardCharsets.UTF_8), attemptId, mode, key);
        if (inserted != 1) {
            throw new ConflictException("PAYMENT_IN_PROGRESS", "El intento requiere revisión antes de continuar.");
        }
        String persisted = jdbc.queryForObject(
                "SELECT request_body FROM checkout_creation_requests WHERE payment_attempt_id = ?", String.class, attemptId);
        return persisted.getBytes(StandardCharsets.UTF_8);
    }
}
