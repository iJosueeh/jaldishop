package com.jaldishop.backend.payment.application.reconciliation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparador programado de conciliación de pagos huérfanos.
 *
 * <p>Por defecto se encuentra DESHABILITADO (matchIfMissing = false).
 * Para activarlo en un ambiente específico se requiere:
 * app.payment.reconciliation.enabled=true</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.payment.reconciliation", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

    private final PaymentReconciliationService reconciliationService;

    public PaymentReconciliationScheduler(PaymentReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(fixedDelayString = "${app.payment.reconciliation.fixed-delay-ms:60000}")
    public void run() {
        log.debug("Ejecutando ciclo programado de conciliación de pagos...");
        try {
            reconciliationService.reconcileOrphanApprovedPayments();
        } catch (Exception e) {
            log.error("Fallo inesperado durante la ejecución programada de conciliación: {}", e.getMessage(), e);
        }
    }
}
