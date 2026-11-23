package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.domain.model.LedgerEntryType;
import com.jaldishop.backend.shared.domain.model.Money;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Servicio de Aplicación para el procesamiento seguro de liquidaciones bancarias.
 * Implementa el patrón Pessimistic Advisory Lock sobre PostgreSQL para evitar carreras
 * y doble desembolso a un mismo comercio.
 */
@Service
public class MerchantPayoutProcessingService {

    private final JdbcTemplate jdbcTemplate;

    public MerchantPayoutProcessingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Procesa la liquidación a cuenta bancaria o billetera digital.
     *
     * @param storeId         Identificador del comercio
     * @param destinationId   Identificador de la cuenta/Yape destino registrada
     * @param requestedAmount Monto solicitado para desembolso
     * @return Identificador del lote de liquidación generado
     */
    @Transactional
    public UUID processPayoutForStore(UUID storeId, UUID destinationId, Money requestedAmount) {
        // 1. Adquirir lock transaccional exclusivo en PostgreSQL a nivel de tienda
        Boolean lockAcquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(hashtext('payout_store_' || ?))",
                Boolean.class,
                storeId.toString()
        );

        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new ConflictException("PAYOUT_IN_PROGRESS", "Ya existe una liquidación en ejecución para esta tienda.");
        }

        // 2. Validar umbral mínimo de retiro (S/ 50.00)
        BigDecimal minimumThreshold = new BigDecimal("50.00");
        if (requestedAmount.amount().compareTo(minimumThreshold) < 0) {
            throw new BusinessRuleException("PAYOUT_BELOW_MINIMUM", "El monto mínimo para solicitar liquidación es S/ 50.00.");
        }

        // 3. Obtener saldo disponible acumulado en el libro mayor
        BigDecimal currentBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(credit_amount - debit_amount), 0) FROM merchant_payout_ledgers WHERE store_id = ?",
                BigDecimal.class,
                storeId
        );

        if (currentBalance == null || currentBalance.compareTo(requestedAmount.amount()) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS", "Saldo insuficiente en libro mayor para realizar el desembolso.");
        }

        UUID batchId = UUID.randomUUID();
        String idempotencyKey = "PAYOUT_" + batchId;
        Money newBalance = Money.pen(currentBalance.subtract(requestedAmount.amount()));

        // 4. Asentar débito en el Libro Mayor Inmutable (Append-Only)
        jdbcTemplate.update(
                """
                INSERT INTO merchant_payout_ledgers 
                (store_id, entry_type, debit_amount, credit_amount, balance_after, currency, reference_note, idempotency_key)
                VALUES (?, ?, ?, 0.00, ?, 'PEN', ?, ?)
                """,
                storeId,
                LedgerEntryType.PAYOUT_WITHDRAWAL.name(),
                requestedAmount.amount(),
                newBalance.amount(),
                "Liquidación transferida Lote #" + batchId,
                idempotencyKey
        );

        // 5. Crear el lote en estado PROCESSING para transferencia bancaria
        jdbcTemplate.update(
                """
                INSERT INTO merchant_payout_batches 
                (id, store_id, destination_id, status, gross_amount, banking_fee, net_disbursed_amount, currency, idempotency_key, scheduled_for)
                VALUES (?, ?, ?, 'PROCESSING', ?, 0.00, ?, 'PEN', ?, CURRENT_DATE)
                """,
                batchId,
                storeId,
                destinationId,
                requestedAmount.amount(),
                requestedAmount.amount(),
                idempotencyKey
        );

        return batchId;
    }
}
