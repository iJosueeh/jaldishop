package com.jaldishop.backend.payment.application.webhook;

import java.util.UUID;

/**
 * Resultado estructurado tras la recepción y validación de una notificación webhook.
 */
public record ProcessMercadoPagoWebhookResult(
        UUID paymentId,
        String providerPaymentId,
        String paymentStatus,
        String attemptStatus,
        boolean processed,
        String message
) {
    public static ProcessMercadoPagoWebhookResult ignored(String reason) {
        return new ProcessMercadoPagoWebhookResult(null, null, null, null, false, reason);
    }

    public static ProcessMercadoPagoWebhookResult success(
            UUID paymentId,
            String providerPaymentId,
            String paymentStatus,
            String attemptStatus,
            String message
    ) {
        return new ProcessMercadoPagoWebhookResult(paymentId, providerPaymentId, paymentStatus, attemptStatus, true, message);
    }
}
