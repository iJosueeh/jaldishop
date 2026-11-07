package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.batchSize = batchSize;
        this.ordersReconciliationEnabled = ordersReconciliationEnabled;
        this.ordersGracePeriodSeconds = ordersGracePeriodSeconds;
        this.ordersBatchSize = ordersBatchSize;
        this.ordersLeaseDurationSeconds = ordersLeaseDurationSeconds;
    }

    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            PaymentReconciliationProcessor processor,
            int gracePeriodSeconds,
            int batchSize
    ) {
        this(paymentRepository, null, processor, null, gracePeriodSeconds, batchSize, false, 120, 20, 300);
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
        Duration leaseDuration = Duration.ofSeconds(ordersLeaseDurationSeconds);

        List<UUID> candidateAttemptIds = paymentAttemptRepository.findEligibleOrderReconciliationAttemptIds(
                cutoffTime, now, ordersBatchSize
        );

        if (candidateAttemptIds.isEmpty()) {
            return new OrderReconciliationBatchSummary(0, 0, 0, 0, 0, List.of());
        }

        log.info("Iniciando lote de conciliación de órdenes para {} candidatos (antigüedad > {}s)",
                candidateAttemptIds.size(), ordersGracePeriodSeconds);

        UUID workerToken = UUID.randomUUID();
        List<OrderReconciliationCandidateResult> results = new ArrayList<>();

        for (UUID attemptId : candidateAttemptIds) {
            try {
                // Fase 1: Claim transaccional corto
                OrderReconciliationClaimResult claim = orderProcessor.claimAttempt(attemptId, workerToken, leaseDuration, now);
                if (!claim.claimed()) {
                    results.add(OrderReconciliationCandidateResult.skipped(attemptId, claim.skipReason()));
                    continue;
                }

                // Fase 2: Consulta HTTP sin transacción de BD
                OrderQueryResult remoteOrder;
                try {
                    remoteOrder = orderProcessor.queryRemoteOrder(claim.providerOrderId());
                } catch (Exception ex) {
                    log.warn("Fallo en consulta remota para orden {} (intento {}): {}",
                            claim.providerOrderId(), attemptId, ex.getMessage());
                    orderProcessor.releaseLeaseOnError(attemptId, workerToken, now);
                    results.add(OrderReconciliationCandidateResult.technicalFailure(attemptId, claim.paymentId(), claim.providerOrderId(), ex.getMessage()));
                    continue;
                }

                // Fase 3: Consolidación transaccional
                OrderReconciliationCandidateResult consResult = orderProcessor.consolidateOrderReconciliation(
                        claim, remoteOrder, workerToken, now
                );
                results.add(consResult);

            } catch (Exception e) {
                log.error("Excepción inesperada al conciliar orden para intento {}: {}", attemptId, e.getMessage(), e);
                results.add(OrderReconciliationCandidateResult.technicalFailure(attemptId, null, null, e.getMessage()));
            }
        }

        OrderReconciliationBatchSummary summary = OrderReconciliationBatchSummary.fromResults(results);
        log.info("Lote de conciliación de órdenes finalizado. Total: {}, Exitosos: {}, Omitidos: {}, Incidencias: {}, Fallos: {}",
                summary.totalCandidates(), summary.successCount(), summary.skippedCount(),
                summary.commercialIncidenceCount(), summary.failureCount());
        return summary;
    }

    public void setOrdersReconciliationEnabled(boolean enabled) {
        this.ordersReconciliationEnabled = enabled;
    }

    public boolean isOrdersReconciliationEnabled() {
        return ordersReconciliationEnabled;
    }
}
