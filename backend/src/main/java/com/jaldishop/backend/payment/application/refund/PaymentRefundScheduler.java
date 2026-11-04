package com.jaldishop.backend.payment.application.refund;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public PaymentRefundScheduler(PaymentRefundService refundService) {
        this.refundService = refundService;
    }

    @Scheduled(fixedDelayString = "${payments.mercadopago.refund-scheduler-interval-ms:60000}")
    public void runRefundBatch() {
        log.info("Iniciando tarea programada de procesamiento de reembolsos pendientes...");
        try {
            refundService.processPendingRefunds(25);
        } catch (Exception ex) {
            log.error("Error durante la ejecución del scheduler de reembolsos: {}", ex.getMessage(), ex);
        }
    }
}
