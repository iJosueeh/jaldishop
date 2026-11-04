package com.jaldishop.backend.payment.application.refund;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "payments.mercadopago.refund-scheduler-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PaymentRefundScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundScheduler.class);

    private final PaymentRefundService refundService;
    private final int batchSize;

    @Autowired
    public PaymentRefundScheduler(
            PaymentRefundService refundService,
            @Value("${payments.mercadopago.refund-scheduler-batch-size:25}") int batchSize
    ) {
        this.refundService = refundService;
        this.batchSize = batchSize;
    }

    public PaymentRefundScheduler(PaymentRefundService refundService) {
        this(refundService, 25);
    }

    @Scheduled(fixedDelayString = "${payments.mercadopago.refund-scheduler-interval-ms:60000}")
    public void runRefundBatch() {
        log.info("Iniciando tarea programada de procesamiento de reembolsos pendientes (batchSize={})...", batchSize);
        try {
            refundService.processPendingRefunds(batchSize);
        } catch (Exception ex) {
            log.error("Error durante la ejecución del scheduler de reembolsos: {}", ex.getMessage(), ex);
        }
    }
}
