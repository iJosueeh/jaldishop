package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentReconciliationProcessor processor;
    private final PaymentOrderReconciliationProcessor orderProcessor;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final int gracePeriodSeconds;
    private final int batchSize;
    private boolean ordersReconciliationEnabled;
    private final int ordersGracePeriodSeconds;
    private final int ordersBatchSize;
    private final int ordersLeaseDurationSeconds;

    @Autowired
    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentReconciliationProcessor processor,
            PaymentOrderReconciliationProcessor orderProcessor,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            @Value("${app.payment.reconciliation.grace-period-seconds:600}") int gracePeriodSeconds,
            @Value("${app.payment.reconciliation.batch-size:50}") int batchSize,
            @Value("${app.payment.reconciliation.orders-enabled:false}") boolean ordersReconciliationEnabled,
            @Value("${app.payment.reconciliation.orders-grace-period-seconds:120}") int ordersGracePeriodSeconds,
            @Value("${app.payment.reconciliation.orders-batch-size:20}") int ordersBatchSize,
            @Value("${app.payment.reconciliation.orders-lease-duration-seconds:300}") int ordersLeaseDurationSeconds
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.processor = processor;
        this.orderProcessor = orderProcessor;
        this.jdbcTemplate = jdbcTemplate;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.batchSize = batchSize;
        this.ordersReconciliationEnabled = ordersReconciliationEnabled;
        this.ordersGracePeriodSeconds = ordersGracePeriodSeconds;
        this.ordersBatchSize = ordersBatchSize;
        this.ordersLeaseDurationSeconds = ordersLeaseDurationSeconds;
    }

    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentReconciliationProcessor processor,
            PaymentOrderReconciliationProcessor orderProcessor,
            int gracePeriodSeconds,
            int batchSize,
            boolean ordersReconciliationEnabled,
            int ordersGracePeriodSeconds,
            int ordersBatchSize,
            int ordersLeaseDurationSeconds
    ) {
        this(paymentRepository, paymentAttemptRepository, processor, orderProcessor, null,
                gracePeriodSeconds, batchSize, ordersReconciliationEnabled, ordersGracePeriodSeconds, ordersBatchSize, ordersLeaseDurationSeconds);
    }

    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            PaymentReconciliationProcessor processor,
            int gracePeriodSeconds,
            int batchSize
    ) {
        this(paymentRepository, null, processor, null, null, gracePeriodSeconds, batchSize, false, 120, 20, 300);
    }

    public ReconciliationBatchSummary reconcileOrphanApprovedPayments() {
        Instant now = Instant.now();
        Instant cutoffTime = now.minusSeconds(gracePeriodSeconds);

        List<UUID> candidateIds = paymentRepository.findEligibleReconciliationPaymentIds(cutoffTime, now, batchSize);
        if (candidateIds.isEmpty()) {
            return new ReconciliationBatchSummary(0, 0, 0, 0, 0, List.of());
        }

        log.info("Iniciando lote de conciliación para {} pagos candidatos (antigüedad > {}s)",
                candidateIds.size(), gracePeriodSeconds);

        List<ReconciliationCandidateResult> results = new ArrayList<>();
        for (UUID paymentId : candidateIds) {
            try {
                ReconciliationCandidateResult result = processor.processCandidate(paymentId);
                results.add(result);
            } catch (Exception e) {
                log.error("Excepción inesperada no interceptada al conciliar pago {}: {}", paymentId, e.getMessage(), e);
                results.add(ReconciliationCandidateResult.technicalFailure(paymentId, e.getMessage()));
            }
        }

        ReconciliationBatchSummary summary = ReconciliationBatchSummary.fromResults(results);
        log.info("Lote de conciliación finalizado. Total: {}, Exitosos: {}, Omitidos: {}, Comerciales fallidos: {}, Técnicos/Reintentos: {}",
                summary.totalCandidates(), summary.successCount(), summary.skippedCount(),
                summary.commercialFailuresCount(), summary.technicalFailuresCount());
        return summary;
    }

    public OrderReconciliationBatchSummary reconcileOrphanOrders() {
        if (!ordersReconciliationEnabled) {
            log.debug("Conciliación activa de órdenes Orders API deshabilitada por configuración");
            return new OrderReconciliationBatchSummary(0, 0, 0, 0, 0, List.of());
        }

        if (paymentAttemptRepository == null || orderProcessor == null) {
            log.warn("paymentAttemptRepository u orderProcessor no inyectados en PaymentReconciliationService");
            return new OrderReconciliationBatchSummary(0, 0, 0, 0, 0, List.of());
        }

        Instant now = Instant.now();
        Instant cutoffTime = now.minusSeconds(ordersGracePeriodSeconds);

        List<UUID> candidateAttemptIds = paymentAttemptRepository.findEligibleOrderReconciliationAttemptIds(
                cutoffTime, now, ordersBatchSize
        );

        if (candidateAttemptIds.isEmpty()) {
            return new OrderReconciliationBatchSummary(0, 0, 0, 0, 0, List.of());
        }

        log.info("Iniciando lote de conciliación de órdenes para {} candidatos (antigüedad > {}s)",
                candidateAttemptIds.size(), ordersGracePeriodSeconds);

        List<OrderReconciliationCandidateResult> results = new ArrayList<>();
        for (UUID attemptId : candidateAttemptIds) {
            try {
                results.add(reconcileOrderAttempt(attemptId));
            } catch (RuntimeException ex) {
                log.error("Fallo al finalizar conciliación de intento {} ({})", attemptId, ex.getClass().getSimpleName());
                results.add(OrderReconciliationCandidateResult.technicalFailure(attemptId, null, null,
                        "No se pudo finalizar la conciliación o recuperar su estado persistido"));
            }
        }

        OrderReconciliationBatchSummary summary = OrderReconciliationBatchSummary.fromResults(results);
        log.info("Lote de conciliación de órdenes finalizado. Total: {}, Exitosos: {}, Omitidos: {}, Incidencias: {}, Fallos: {}",
                summary.totalCandidates(), summary.successCount(), summary.skippedCount(),
                summary.commercialIncidenceCount(), summary.failureCount());
        return summary;
    }

    /** Manual entry point; deliberately independent of the scheduled batch feature flag. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderReconciliationCandidateResult reconcileOrderAttempt(UUID attemptId) {
        UUID workerToken = UUID.randomUUID();
        OrderReconciliationCandidateResult result;
        try {
            result = orderProcessor.reconcileApprovedAttempt(attemptId);
            if (result == null) {
                var claim = orderProcessor.claimAttempt(attemptId, workerToken,
                        Duration.ofSeconds(ordersLeaseDurationSeconds), Instant.now());
                if (!claim.claimed()) {
                    result = OrderReconciliationCandidateResult.skipped(attemptId, claim.skipReason());
                } else {
                    // HTTP outside a transaction; evaluate lease ownership with the post-HTTP time.
                    var remote = orderProcessor.queryRemoteOrder(claim.providerOrderId());
                    result = orderProcessor.consolidateOrderReconciliation(claim, remote, workerToken, Instant.now());
                }
            }
        } catch (Exception ex) {
            log.error("Fallo al conciliar intento {} ({})", attemptId, ex.getClass().getSimpleName());
            result = OrderReconciliationCandidateResult.technicalFailure(attemptId, null, null,
                    ex instanceof com.jaldishop.backend.shared.exception.ConflictException conflict
                            ? "Validación rechazada: " + conflict.getCode()
                            : ex instanceof SecurityException ? "Cuenta o entorno remoto no autorizado"
                            : "No se completó la conciliación; consultar estado financiero y comercial persistido");
        } finally {
            // Only this token may release the claim, including on validation/consolidation failures.
            orderProcessor.releaseLeaseOnError(attemptId, workerToken, Instant.now());
        }
        var attempt = paymentAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) return result;
        var payment = paymentRepository.findById(attempt.getPaymentId()).orElse(null);
        return new OrderReconciliationCandidateResult(attemptId, attempt.getPaymentId(), attempt.getProviderOrderId(),
                result.status(), result.detail(), payment == null ? null : payment.getStatus().name(),
                payment != null && payment.getStatus() == PaymentStatus.APPROVED
                        && result.commercialStatus() == OrderReconciliationCandidateResult.CommercialStatus.NOT_ATTEMPTED
                        ? OrderReconciliationCandidateResult.CommercialStatus.PENDING : result.commercialStatus());
    }

    public void setOrdersReconciliationEnabled(boolean enabled) {
        this.ordersReconciliationEnabled = enabled;
    }

    public boolean isOrdersReconciliationEnabled() {
        return ordersReconciliationEnabled;
    }

    public List<com.jaldishop.backend.payment.web.dto.ReconciliationCandidateItemResponse> findOrphanCandidates(int limit) {
        if (jdbcTemplate == null) {
            return List.of();
        }
        int effectiveLimit = Math.max(1, Math.min(limit, 50));
        String sql = """
            SELECT
                COALESCE(pa.id, p.id) AS attempt_id,
                p.id AS payment_id,
                pa.provider_order_id,
                pa.provider_payment_id,
                p.amount,
                p.currency,
                COALESCE(pa.payment_method, 'CARD') AS payment_method,
                COALESCE(s.name, 'Tienda Desconocida') AS store_name,
                COALESCE(u.email, 'Sin correo') AS customer_email,
                COALESCE(TRIM(CONCAT(u.first_name, ' ', u.last_name)), 'Cliente') AS customer_name,
                p.approved_at,
                'ORPHAN_APPROVED' AS incidence_type
            FROM payments p
            LEFT JOIN payment_attempts pa ON pa.payment_id = p.id
            LEFT JOIN capacity_reservations cr ON cr.id = p.capacity_reservation_id
            LEFT JOIN stores s ON s.id = cr.store_id
            LEFT JOIN users u ON u.id = cr.user_id
            WHERE p.status = 'APPROVED'
              AND p.refund_status = 'NOT_REQUIRED'
              AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.payment_id = p.id)
              AND NOT EXISTS (
                  SELECT 1 FROM payment_incidences pi
                  WHERE pi.payment_id = p.id
                    AND pi.status IN ('MANUAL_REVIEW_REQUIRED', 'RESOLVED')
              )
            ORDER BY p.approved_at DESC NULLS LAST, p.created_at DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new com.jaldishop.backend.payment.web.dto.ReconciliationCandidateItemResponse(
                (UUID) rs.getObject("attempt_id"),
                (UUID) rs.getObject("payment_id"),
                rs.getString("provider_order_id"),
                rs.getString("provider_payment_id"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("payment_method"),
                rs.getString("store_name"),
                rs.getString("customer_email"),
                rs.getString("customer_name"),
                rs.getTimestamp("approved_at") != null ? rs.getTimestamp("approved_at").toInstant() : null,
                rs.getString("incidence_type")
        ), effectiveLimit);
    }
}
