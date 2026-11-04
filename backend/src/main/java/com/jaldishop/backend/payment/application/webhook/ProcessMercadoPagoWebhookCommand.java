package com.jaldishop.backend.payment.application.webhook;

import java.util.Map;

/**
 * Comando que encapsula los datos recibidos en la solicitud HTTP del webhook.
 */
public record ProcessMercadoPagoWebhookCommand(
        String xSignature,
        String xRequestId,
        Map<String, String> queryParams,
        String rawBody
) {}
