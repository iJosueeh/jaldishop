package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookCommand;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookResult;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookService;
import com.jaldishop.backend.shared.exception.ApiError;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador HTTP público para la recepción de Webhooks de Mercado Pago.
 * Autenticado criptográficamente mediante la cabecera x-signature.
 */
@RestController
@RequestMapping("/api/v1/checkout/mercado-pago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final ProcessMercadoPagoWebhookService webhookService;

    public MercadoPagoWebhookController(ProcessMercadoPagoWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) String rawBody
    ) {
        log.info("Recibida notificación webhook de Mercado Pago. x-request-id: {}", xRequestId);

        try {
            ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                    xSignature,
                    xRequestId,
                    queryParams,
                    rawBody
            );

            ProcessMercadoPagoWebhookResult result = webhookService.execute(command);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "processed", result.processed(),
                    "message", result.message() != null ? result.message() : "Notificación recibida exitosamente."
            ));

        } catch (SecurityException ex) {
            log.warn("Firma inválida o ausente en el webhook: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiError("INVALID_WEBHOOK_SIGNATURE", ex.getMessage())
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Parámetros de webhook incompletos o malformados: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiError("BAD_WEBHOOK_REQUEST", ex.getMessage())
            );
        } catch (ConflictException ex) {
            log.warn("Conflicto al procesar notificación webhook: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ApiError(ex.getCode(), ex.getMessage())
            );
        }
    }

}
