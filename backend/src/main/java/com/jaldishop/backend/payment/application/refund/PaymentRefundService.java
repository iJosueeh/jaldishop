package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundClaimResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundConsolidationResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundExecutionResult;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentRefundService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundProcessor refundProcessor;

    @Autowired
    public PaymentRefundService(
            PaymentRepository paymentRepository,
            PaymentRefundProcessor refundProcessor
    ) {
        this.paymentRepository = paymentRepository;
        this.refundProcessor = refundProcessor;
    }

    public RefundBatchSummary processPendingRefunds(int limit) {
        UUID workerToken = UUID.randomUUID();
        log.info("Iniciando ciclo de procesamiento de reembolsos con workerToken: {}", workerToken);

        int batchSize = limit > 0 ? limit : 20;
        List<UUID> candidateIds = paymentRepository.findEligibleRefundPaymentIds(java.time.Instant.now(), batchSize);

        int total = candidateIds.size();
        int completed = 0;
        int skipped = 0;
        int failed = 0;
        int indeterminate = 0;

        for (UUID paymentId : candidateIds) {
            try {
                // Fase 1: Reclamación con lease
                RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(paymentId, workerToken);
                if (!claim.claimed()) {
                    log.info("Pago {} omitido para reembolso: {}", paymentId, claim.skipReason());
                    skipped++;
                    continue;
                }

                // Fase 2: Llamada HTTP fuera de transacción JDBC
                RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);

                // Fase 3: Consolidación transaccional corta con validación de propiedad de claim
                RefundConsolidationResult consResult = refundProcessor.consolidateRefundResult(claim, execResult);

                switch (consResult.status()) {
                    case COMPLETED -> completed++;
                    case FAILED -> failed++;
                    case INDETERMINATE -> indeterminate++;
                    case CLAIM_LOST, ERROR, SUSPENDED -> skipped++;
                }

            } catch (Exception ex) {
                log.error("Excepción inesperada al procesar reembolso para pago {}: {}", paymentId, ex.getMessage(), ex);
                indeterminate++;
            }
        }

        log.info("Ciclo de reembolsos finalizado. Total: {}, Completados: {}, Omitidos: {}, Fallidos: {}, Indeterminados: {}",
                total, completed, skipped, failed, indeterminate);

        return new RefundBatchSummary(total, completed, skipped, failed, indeterminate);
    }

    public record RefundBatchSummary(
            int totalProcessed,
            int completedCount,
            int skippedCount,
            int failedCount,
            int indeterminateCount
    ) {}
}
