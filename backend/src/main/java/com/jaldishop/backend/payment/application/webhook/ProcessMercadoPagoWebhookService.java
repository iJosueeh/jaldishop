package com.jaldishop.backend.payment.application.webhook;

import tools.jackson.databind.ObjectMapper;
import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.payment.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import com.jaldishop.backend.payment.web.dto.MercadoPagoWebhookPayload;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestador de aplicación para la recepción y procesamiento de Webhooks de Mercado Pago.
 * Ejecuta la verificación criptográfica HMAC, consulta el estado real en Mercado Pago
 * (estrictamente fuera de transacción de base de datos) y delega la persistencia.
 */
@Service
public class ProcessMercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ProcessMercadoPagoWebhookService.class);

    private final MercadoPagoWebhookSignatureValidator signatureValidator;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PaymentWebhookPersistenceService persistenceService;
    private final MercadoPagoProperties properties;
    private final ObjectMapper objectMapper;
    private final ConfirmPurchaseService confirmPurchaseService;
    private final OrderRepository orderRepository;

    @Autowired
    public ProcessMercadoPagoWebhookService(
            MercadoPagoWebhookSignatureValidator signatureValidator,
            MercadoPagoGateway mercadoPagoGateway,
            PaymentWebhookPersistenceService persistenceService,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper,
            ConfirmPurchaseService confirmPurchaseService,
            OrderRepository orderRepository
    ) {
        this.signatureValidator = signatureValidator;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.persistenceService = persistenceService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.confirmPurchaseService = confirmPurchaseService;
        this.orderRepository = orderRepository;
    }

    public ProcessMercadoPagoWebhookService(
            MercadoPagoWebhookSignatureValidator signatureValidator,
            MercadoPagoGateway mercadoPagoGateway,
            PaymentWebhookPersistenceService persistenceService,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper,
            ConfirmPurchaseService confirmPurchaseService
    ) {
        this(signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, confirmPurchaseService, null);
    }

    public ProcessMercadoPagoWebhookService(
            MercadoPagoWebhookSignatureValidator signatureValidator,
            MercadoPagoGateway mercadoPagoGateway,
            PaymentWebhookPersistenceService persistenceService,
            MercadoPagoProperties properties,
            ObjectMapper objectMapper
    ) {
        this(signatureValidator, mercadoPagoGateway, persistenceService, properties, objectMapper, null, null);
    }

    public ProcessMercadoPagoWebhookResult execute(ProcessMercadoPagoWebhookCommand command) {
        // 1. Extracción y Validación de Identificador de Evento (data.id o id)
        Map<String, String> queryParams = command.queryParams() != null ? command.queryParams() : Map.of();
        String queryDataId = queryParams.get("data.id");
        if (queryDataId == null || queryDataId.isBlank()) {
            queryDataId = queryParams.get("id");
        }

        String bodyDataId = null;
        MercadoPagoWebhookPayload payload = null;
        if (command.rawBody() != null && !command.rawBody().isBlank()) {
            try {
                payload = objectMapper.readValue(command.rawBody(), MercadoPagoWebhookPayload.class);
                if (payload.data() != null && payload.data().id() != null && !payload.data().id().isBlank()) {
                    bodyDataId = payload.data().id();
                } else if (payload.id() != null && !payload.id().isBlank()) {
                    bodyDataId = payload.id();
                }
            } catch (Exception e) {
                log.warn("Cuerpo de webhook malformado o no parseable: {}", e.getMessage());
            }
        }

        // H-02: Consistencia entre parámetros de consulta y cuerpo JSON
        if (queryDataId != null && !queryDataId.isBlank() && bodyDataId != null && !bodyDataId.isBlank()) {
            if (!queryDataId.equalsIgnoreCase(bodyDataId)) {
                log.warn("Discrepancia detectada en identificadores de webhook: query dataId={} vs body dataId={}",
                        queryDataId, bodyDataId);
                throw new IllegalArgumentException("Identificadores de evento inconsistentes entre parámetros de consulta y cuerpo JSON.");
            }
        }

        String dataId = (queryDataId != null && !queryDataId.isBlank()) ? queryDataId : bodyDataId;

        if (dataId == null || dataId.isBlank()) {
            throw new IllegalArgumentException("Identificador de evento no proporcionado en la notificación (falta data.id o id).");
        }

        // 2. Autenticación y Verificación Criptográfica de la Notificación
        boolean validSignature = signatureValidator.isValid(
                command.xSignature(),
                command.xRequestId(),
                dataId,
                properties.getWebhookSecret()
        );

        if (!validSignature) {
            log.warn("Rechazando webhook de Mercado Pago por firma inválida o encabezados faltantes.");
            throw new SecurityException("Firma criptográfica x-signature inválida o encabezados requeridos ausentes.");
        }

        // 3. Filtrado de Eventos Soportados (Únicamente pagos)
        String type = queryParams.get("type");
        if (type == null && queryParams.get("topic") != null) {
            type = queryParams.get("topic");
        }
        if (type == null && payload != null) {
            type = payload.type();
        }
        String action = payload != null ? payload.action() : null;

        boolean isPaymentEvent = "payment".equalsIgnoreCase(type)
                || (action != null && action.toLowerCase().startsWith("payment."));

        if (!isPaymentEvent) {
            log.info("Evento no relacionado con pagos recibido ({}, action: {}). Notificación ignorada de forma segura.",
                    type, action);
            return ProcessMercadoPagoWebhookResult.ignored("Tipo de evento no soportado: " + type);
        }

        // 4. Consulta Directa a Mercado Pago (FUERA de transacción de PostgreSQL)
        log.info("Consultando estado real del pago {} en Mercado Pago...", dataId);
        PaymentQueryResult queryResult = mercadoPagoGateway.queryPayment(dataId);

        // H-04: Verificación de Cuenta Receptora (collector_id) si está configurada
        if (properties.getExpectedCollectorId() != null && properties.getExpectedCollectorId() > 0) {
            if (queryResult.collectorId() == null || !properties.getExpectedCollectorId().equals(queryResult.collectorId())) {
                log.error("Rechazando pago {}: collector_id recibido ({}) no coincide con el comercio esperado ({})",
                        dataId, queryResult.collectorId(), properties.getExpectedCollectorId());
                throw new SecurityException("El pago pertenece a una cuenta receptora no autorizada (collector_id=" + queryResult.collectorId() + ").");
            }
        }

        // H-04: Verificación de Entorno (live_mode) si está configurado
        if (properties.getExpectedLiveMode() != null) {
            if (queryResult.liveMode() == null || !properties.getExpectedLiveMode().equals(queryResult.liveMode())) {
                log.error("Rechazando pago {}: live_mode recibido ({}) no coincide con el entorno esperado ({})",
                        dataId, queryResult.liveMode(), properties.getExpectedLiveMode());
                throw new SecurityException("El entorno del pago (live_mode=" + queryResult.liveMode() + ") no coincide con el entorno esperado.");
            }
        }

        // 5. Verificación de external_reference
        if (queryResult.externalReference() == null || queryResult.externalReference().isBlank()) {
            log.error("El pago {} en Mercado Pago no contiene external_reference para asociar a JaldiShop.", dataId);
            throw new ConflictException("MISSING_EXTERNAL_REFERENCE",
                    "El pago consultado no posee external_reference válida vinculada a un pago local.");
        }

        UUID paymentId;
        try {
            paymentId = UUID.fromString(queryResult.externalReference());
        } catch (IllegalArgumentException e) {
            log.error("external_reference '{}' del pago {} no es un UUID válido.", queryResult.externalReference(), dataId);
            throw new ConflictException("INVALID_EXTERNAL_REFERENCE",
                    "external_reference no posee un formato de UUID válido.");
        }

        // 6. Transición y Persistencia Financiera en DB (Transacción 1 corta aislada)
        ProcessMercadoPagoWebhookResult persistenceResult = persistenceService.applyQueryResultToPayment(paymentId, queryResult);

        // 7. Confirmación Comercial de Pedido (Transacción 2 separada)
        if ("APPROVED".equalsIgnoreCase(persistenceResult.paymentStatus()) && confirmPurchaseService != null) {
            try {
                OrderConfirmation orderConfirmation = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));
                log.info("Pedido {} confirmado exitosamente para el pago {}", orderConfirmation.orderNumber(), paymentId);
                return ProcessMercadoPagoWebhookResult.success(
                        paymentId,
                        queryResult.providerPaymentId(),
                        "APPROVED",
                        persistenceResult.attemptStatus(),
                        "Pago y pedido (" + orderConfirmation.orderNumber() + ") confirmados exitosamente."
                );
            } catch (ConflictException ex) {
                // A. Fallos comerciales definitivos (reserva expirada, stock insuficiente, snapshot inconsistente, tienda inactiva)
                // Se registra incidencia recuperable y refundStatus=PENDING; se responde 200 para evitar reintentos fútiles de MP.
                log.warn("Fallo comercial definitivo en confirmación para pago APPROVED {}: [{}] {}",
                        paymentId, ex.getCode(), ex.getMessage());
                persistenceService.recordApprovalIncidence(paymentId, ex.getMessage());
                return ProcessMercadoPagoWebhookResult.success(
                        paymentId,
                        queryResult.providerPaymentId(),
                        "APPROVED",
                        persistenceResult.attemptStatus(),
                        "Pago aprobado financieramente, pero la confirmación comercial falló (" + ex.getMessage() + "). Incidencia registrada para conciliación."
                );
            } catch (DataIntegrityViolationException ex) {
                // C. Carrera de concurrencia: colisión de unicidad (uq_orders_payment_id o uq_orders_capacity_reservation_id)
                log.warn("Violación de integridad/unicidad al confirmar pedido para pago {}. Verificando ganador concurrente...", paymentId);
                if (orderRepository != null) {
                    Optional<Order> existingOrder = orderRepository.findByPaymentId(paymentId);
                    if (existingOrder.isPresent()) {
                        Order order = existingOrder.get();
                        log.info("Resolución de carrera exitosa: pedido {} ya fue creado concurrentemente para pago {}",
                                order.getOrderNumber(), paymentId);
                        return ProcessMercadoPagoWebhookResult.success(
                                paymentId,
                                queryResult.providerPaymentId(),
                                "APPROVED",
                                persistenceResult.attemptStatus(),
                                "Pago y pedido (" + order.getOrderNumber() + ") confirmados exitosamente (resolución concurrente)."
                        );
                    }
                }
                log.error("Violación de integridad sin orden previa para pago APPROVED {}: {}. No se marca reembolso automático; se conserva APPROVED para reintento/revisión.",
                        paymentId, ex.getMessage());
                throw ex;
            } catch (TransientDataAccessException ex) {
                // B. Fallos técnicos transitorios (CannotAcquireLockException, deadlocks, fallos temporales de red/DB)
                // NO se marca refundStatus=PENDING. El pago permanece APPROVED.
                // Se propaga para responder 5xx a Mercado Pago y forzar reintento automático.
                log.error("Fallo transitorio de base de datos confirmando pedido para pago APPROVED {}. Se reintentará vía webhook: {}",
                        paymentId, ex.getMessage());
                throw ex;
            } catch (RuntimeException ex) {
                // D. Errores inesperados:
                // NO convertirlos automáticamente en fallos comerciales ni en reembolsos.
                // Payment permanece APPROVED. Se propaga para revisión y no emitir falso 200.
                log.error("Error inesperado al confirmar pedido para pago APPROVED {}. No se marca reembolso; se propaga: {}",
                        paymentId, ex.getMessage(), ex);
                throw ex;
            }
        }

        return persistenceResult;
    }

}
