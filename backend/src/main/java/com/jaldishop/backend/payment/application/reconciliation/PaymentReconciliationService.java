package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentReconciliationProcessor processor;
    private final int gracePeriodSeconds;
    private final int batchSize;

    public PaymentReconciliationService(
            PaymentRepository paymentRepository,
            PaymentReconciliationProcessor processor,
            @Value("${app.payment.reconciliation.grace-period-seconds:600}") int gracePeriodSeconds,
            @Value("${app.payment.reconciliation.batch-size:50}") int batchSize
    ) {
        this.paymentRepository = paymentRepository;
        this.processor = processor;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.batchSize = batchSize;
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
}
