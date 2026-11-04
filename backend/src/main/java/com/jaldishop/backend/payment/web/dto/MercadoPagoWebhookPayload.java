package com.jaldishop.backend.payment.web.dto;

/**
 * Representación del cuerpo JSON enviado en las notificaciones Webhook de Mercado Pago.
 */
public record MercadoPagoWebhookPayload(
        String id,
        Boolean liveMode,
        String type,
        String dateCreated,
        Long userId,
        String apiVersion,
        String action,
        DataId data
) {
    public record DataId(
            String id
    ) {}
}
