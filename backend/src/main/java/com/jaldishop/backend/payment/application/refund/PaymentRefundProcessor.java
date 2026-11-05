package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;
import com.jaldishop.backend.payment.application.reconciliation.PaymentIncidenceRecorder;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRefundProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundProcessor.class);
    public static final Duration LEASE_DURATION = Duration.ofMinutes(10);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final PaymentRefundIntentRepository refundIntentRepository;
    private final PaymentRefundExecutionAttemptRepository executionAttemptRepository;
    private final PaymentIncidenceRepository incidenceRepository;
    private final MercadoPagoGateway mercadoPagoGateway;

    @Autowired
    public PaymentRefundProcessor(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            OrderRepository orderRepository,
            PaymentRefundIntentRepository refundIntentRepository,
            PaymentRefundExecutionAttemptRepository executionAttemptRepository,
            PaymentIncidenceRepository incidenceRepository,
            MercadoPagoGateway mercadoPagoGateway
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.orderRepository = orderRepository;
        this.refundIntentRepository = refundIntentRepository;
        this.executionAttemptRepository = executionAttemptRepository;
        this.incidenceRepository = incidenceRepository;
        this.mercadoPagoGateway = mercadoPagoGateway;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundClaimResult claimOrCreateRefundIntent(UUID paymentId, UUID workerToken) {
        Instant now = Instant.now();

        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) {
            return RefundClaimResult.skipped("Pago inexistente: " + paymentId);
        }

        if (!payment.isApproved()) {
            return RefundClaimResult.skipped("El pago no se encuentra en estado APPROVED");
        }
        if (payment.getRefundStatus() != RefundStatus.PENDING && payment.getRefundStatus() != RefundStatus.PROCESSING) {
            return RefundClaimResult.skipped("El pago no está pendiente ni en procesamiento de reembolso: " + payment.getRefundStatus());
        }

        if (incidenceRepository != null) {
            Optional<PaymentIncidence> activeIncidenceOpt = incidenceRepository.findActiveByPaymentIdAndProcessType(
                    paymentId, IncidenceProcessType.REFUND_EXECUTION
            );
            if (activeIncidenceOpt.isPresent()) {
                PaymentIncidence activeIncidence = activeIncidenceOpt.get();
                if (activeIncidence.getStatus() == IncidenceStatus.MANUAL_REVIEW_REQUIRED) {
                    log.info("Reembolso omitido: el pago {} requiere revisión manual (MANUAL_REVIEW_REQUIRED)", paymentId);
                    return RefundClaimResult.skipped("Reembolso requiere revisión manual (MANUAL_REVIEW_REQUIRED)");
                }
                if (activeIncidence.getNextRetryAt() != null && activeIncidence.getNextRetryAt().isAfter(now)) {
                    log.info("Reembolso omitido: el pago {} tiene reintento vigente a futuro (nextRetryAt: {})",
                            paymentId, activeIncidence.getNextRetryAt());
                    return RefundClaimResult.skipped("Reembolso en período de espera de backoff");
                }
                if (activeIncidence.getAttemptsCount() >= activeIncidence.getMaxAttempts()) {
                    log.info("Reembolso omitido: el pago {} ha superado el máximo de intentos permitidos ({})",
                            paymentId, activeIncidence.getMaxAttempts());
                    return RefundClaimResult.skipped("Reembolso superó el máximo de intentos permitidos");
                }
            }
        }

        Optional<Order> orderOpt = orderRepository.findByPaymentId(paymentId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getStatus() != OrderStatus.CANCELLED) {
                log.error("Anomalía: el pago {} ya tiene un pedido confirmado activo (número: {}, estado: {}). Abortando solicitud de reembolso.",
                        paymentId, order.getOrderNumber(), order.getStatus());
                return RefundClaimResult.skipped("El pago ya tiene pedido confirmado activo asociado");
            }
            log.info("Pago {} corresponde a un pedido legítimamente cancelado (número: {}, estado: {}). Procediendo con la intención de reembolso.",
                    paymentId, order.getOrderNumber(), order.getStatus());
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId);
        List<PaymentAttempt> successfulAttempts = attempts.stream()
                .filter(a -> a.getStatus() == PaymentAttemptStatus.APPROVED && (
                        (a.getProviderPaymentId() != null && !a.getProviderPaymentId().isBlank()) ||
                        (a.getProviderOrderId() != null && !a.getProviderOrderId().isBlank())
                ))
                .toList();

        if (successfulAttempts.isEmpty()) {
            log.error("Anomalía financiera: Pago {} en APPROVED no tiene ningún PaymentAttempt exitoso con identificador de proveedor", paymentId);
            recordRefundIncidence(paymentId, IncidenceCategory.OPERATIONAL, "NO_SUCCESSFUL_ATTEMPT", "Pago aprobado sin intento exitoso", IncidenceStatus.MANUAL_REVIEW_REQUIRED, now);
            return RefundClaimResult.skipped("Sin intento exitoso para reembolsar");
        }

        if (successfulAttempts.size() > 1) {
            log.error("ANOMALÍA FINANCIERA CRÍTICA: Pago {} tiene múltiples intentos exitosos ({}). Escalar a revisión manual.",
                    paymentId, successfulAttempts.size());
            recordRefundIncidence(paymentId, IncidenceCategory.OPERATIONAL, "MULTIPLE_SUCCESSFUL_ATTEMPTS", "Múltiples intentos aprobados para un mismo pago", IncidenceStatus.MANUAL_REVIEW_REQUIRED, now);
            return RefundClaimResult.skipped("Múltiples intentos exitosos detectados");
        }

        PaymentAttempt targetAttempt = successfulAttempts.getFirst();
        String checkoutMode = targetAttempt.getCheckoutMode() != null && !targetAttempt.getCheckoutMode().isBlank()
                ? targetAttempt.getCheckoutMode() : "PREFERENCES";
        String providerOrderId = targetAttempt.getProviderOrderId();
        String providerPaymentId = targetAttempt.getProviderPaymentId();
        BigDecimal refundAmount = payment.getRefundAmount() != null ? payment.getRefundAmount() : payment.getAmount();

        if ("ORDERS".equalsIgnoreCase(checkoutMode) && (providerOrderId == null || providerOrderId.isBlank())) {
            log.error("Anomalía operativa: Intento exitoso en modo ORDERS para pago {} no posee providerOrderId", paymentId);
            recordRefundIncidence(paymentId, IncidenceCategory.OPERATIONAL, "MISSING_PROVIDER_ORDER_ID", "Intento ORDERS sin providerOrderId", IncidenceStatus.MANUAL_REVIEW_REQUIRED, now);
            return RefundClaimResult.skipped("Intento ORDERS sin providerOrderId");
        }

        Optional<PaymentRefundIntent> existingOpt = refundIntentRepository.findByPaymentIdForUpdate(paymentId);
        PaymentRefundIntent intent;
        RefundIntentStatus previousStatus = RefundIntentStatus.PENDING;

        if (existingOpt.isEmpty()) {
            String stableIdempotencyKey = "refund-" + paymentId;
            intent = PaymentRefundIntent.create(
                    paymentId,
                    checkoutMode,
                    providerOrderId,
                    providerPaymentId,
                    refundAmount,
                    payment.getCurrency(),
                    stableIdempotencyKey,
                    workerToken,
                    LEASE_DURATION,
                    now
            );
            intent = refundIntentRepository.save(intent);

            payment.markRefundProcessing();
            paymentRepository.save(payment);
            log.info("Intención de reembolso creada para el pago {} con clave {} en modo {}",
                    paymentId, stableIdempotencyKey, checkoutMode);
        } else {
            intent = existingOpt.get();
            previousStatus = intent.getStatus();
            if (intent.getStatus() == RefundIntentStatus.COMPLETED) {
                if (payment.getRefundStatus() != RefundStatus.COMPLETED) {
                    payment.completeRefund(intent.getProviderRefundId(), intent.getCompletedAt());
                    paymentRepository.save(payment);
                }
                return RefundClaimResult.skipped("Reembolso ya completado previamente");
            }
            if (intent.getStatus() == RefundIntentStatus.FAILED) {
                return RefundClaimResult.skipped("Reembolso marcado previamente como fallido definitivo");
            }
            if (!intent.canBeClaimed(now)) {
                log.info("El pago {} está siendo procesado por otro worker con lease activo hasta {}",
                        paymentId, intent.getLeaseExpiresAt());
                return RefundClaimResult.skipped("Lease activo de otro worker");
            }

            intent.acquireClaim(workerToken, LEASE_DURATION, now);
            intent = refundIntentRepository.save(intent);

            payment.markRefundProcessing();
            paymentRepository.save(payment);
            log.info("Lease de reembolso renovado para el pago {} por worker {}", paymentId, workerToken);
        }

        String effectiveCheckoutMode = intent.getCheckoutMode() != null ? intent.getCheckoutMode() : checkoutMode;
        String effectiveProviderOrderId = intent.getProviderOrderId() != null ? intent.getProviderOrderId() : providerOrderId;
        String effectiveProviderPaymentId = intent.getProviderPaymentId() != null ? intent.getProviderPaymentId() : providerPaymentId;

        return RefundClaimResult.claimed(
                paymentId,
                intent.getId(),
                workerToken,
                effectiveCheckoutMode,
                effectiveProviderOrderId,
                effectiveProviderPaymentId,
                refundAmount,
                intent.getIdempotencyKey(),
                previousStatus
        );
    }

    public RefundExecutionResult executeExternalRefund(RefundClaimResult claim) {
        log.info("Iniciando llamada externa de reembolso para pago {} (mode: {}, providerOrderId: {}, providerPaymentId: {})",
                claim.paymentId(), claim.checkoutMode(), claim.providerOrderId(), claim.providerPaymentId());

        if (claim.previousStatus() == RefundIntentStatus.UNKNOWN || claim.previousStatus() == RefundIntentStatus.PROCESSING) {
            log.info("Consultando al proveedor antes de intentar POST para el pago {}", claim.paymentId());
            try {
                if ("ORDERS".equalsIgnoreCase(claim.checkoutMode())) {
                    OrderQueryResult orderQuery = mercadoPagoGateway.queryOrder(claim.providerOrderId());
                    boolean isRefunded = "refunded".equalsIgnoreCase(orderQuery.statusDetail())
                            || "partially_refunded".equalsIgnoreCase(orderQuery.statusDetail())
                            || (orderQuery.refunds() != null && orderQuery.refunds().stream().anyMatch(r -> "approved".equalsIgnoreCase(r.status())));

                    if (isRefunded) {
                        String refundId = orderQuery.refunds() != null
                                ? orderQuery.refunds().stream()
                                        .filter(r -> "approved".equalsIgnoreCase(r.status()))
                                        .map(OrderQueryResult.OrderRefundItem::providerRefundId)
                                        .findFirst()
                                        .orElse(orderQuery.orderId())
                                : orderQuery.orderId();
                        BigDecimal refAmount = orderQuery.refunds() != null
                                ? orderQuery.refunds().stream()
                                        .filter(r -> "approved".equalsIgnoreCase(r.status()))
                                        .map(OrderQueryResult.OrderRefundItem::amount)
                                        .filter(java.util.Objects::nonNull)
                                        .findFirst()
                                        .orElse(claim.amount())
                                : claim.amount();

                        log.info("Reembolso {} ya existente en Orders API para orden {} (pago {})",
                                refundId, claim.providerOrderId(), claim.paymentId());
                        return RefundExecutionResult.success(
                                "GET", 200, refundId, refAmount,
                                orderQuery.statusDetail(), "Reembolso confirmado vía consulta previa de orden"
                        );
                    }
                } else {
                    List<RefundQueryResult> existingRefunds = mercadoPagoGateway.queryPaymentRefunds(claim.providerPaymentId());
                    Optional<RefundQueryResult> approvedRefund = existingRefunds.stream()
                            .filter(r -> "approved".equalsIgnoreCase(r.status()))
                            .findFirst();

                    if (approvedRefund.isPresent()) {
                        RefundQueryResult existing = approvedRefund.get();
                        log.info("Reembolso {} ya existente y aprobado en Mercado Pago para pago {}",
                                existing.providerRefundId(), claim.paymentId());
                        return RefundExecutionResult.success(
                                "GET", 200, existing.providerRefundId(), existing.amount(),
                                "approved", "Reembolso confirmado vía consulta GET previa"
                        );
                    }
                }
                log.info("Consulta previa GET no reportó reembolsos para el pago {}. Procediendo al POST con la misma clave.",
                        claim.paymentId());
            } catch (Exception ex) {
                log.warn("Fallo en consulta previa GET para pago {}: {}. Continuando con precaución...",
                        claim.paymentId(), ex.getMessage());
            }
        }

        try {
            ExecuteRefundCommand command = new ExecuteRefundCommand(
                    claim.checkoutMode(),
                    claim.providerOrderId(),
                    claim.providerPaymentId(),
                    claim.amount(),
                    claim.idempotencyKey()
            );
            RefundResult result = mercadoPagoGateway.executeRefund(command);

            return RefundExecutionResult.success(
                    "POST", 201, result.providerRefundId(), result.amount(),
                    result.status(), "Reembolso ejecutado exitosamente en Mercado Pago"
            );

        } catch (MercadoPagoException ex) {
            if (isDefinitiveCommercialError(ex.getStatusCode(), ex.getErrorCode(), ex.getMessage())) {
                log.error("Rechazo comercial definitivo de Mercado Pago (HTTP {}) para pago {}: {}",
                        ex.getStatusCode(), claim.paymentId(), ex.getMessage());
                return RefundExecutionResult.definitiveFailure("POST", ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());
            } else if (isOperationalAuthOrPermissionError(ex.getStatusCode())) {
                log.error("Fallo operativo de autenticación/permisos en Mercado Pago (HTTP {}) para pago {}: {}",
                        ex.getStatusCode(), claim.paymentId(), ex.getMessage());
                return RefundExecutionResult.operationalSuspension("POST", ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());
            } else {
                log.warn("Error técnico / indeterminado de Mercado Pago (HTTP {}) para pago {}: {}",
                        ex.getStatusCode(), claim.paymentId(), ex.getMessage());
                return RefundExecutionResult.indeterminate("POST", ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());
            }
        } catch (Exception ex) {
            log.error("Excepción inesperada al invocar reembolso en Mercado Pago para pago {}: {}",
                    claim.paymentId(), ex.getMessage());
            return RefundExecutionResult.indeterminate("POST", 500, "UNEXPECTED_ERROR", ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundConsolidationResult consolidateRefundResult(RefundClaimResult claim, RefundExecutionResult execResult) {
        Instant now = Instant.now();
        UUID paymentId = claim.paymentId();

        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
        PaymentRefundIntent intent = refundIntentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);

        if (payment == null || intent == null) {
            log.error("Pago {} o intención de reembolso no encontrados durante consolidación", paymentId);
            return RefundConsolidationResult.error("Entidad no encontrada");
        }

        if (!intent.isClaimOwnedBy(claim.claimToken(), now)) {
            log.warn("CLAIM OBSOLETO: El worker con token {} perdió el lease para el pago {}. Consolidación rechazada.",
                    claim.claimToken(), paymentId);
            return RefundConsolidationResult.claimLost("El lease expiró o fue reclamado por otro worker");
        }

        int nextAttempt = executionAttemptRepository.findNextAttemptNumber(intent.getId());
        String auditStatus = switch (execResult.status()) {
            case SUCCESSFUL -> "SUCCESSFUL";
            case FAILED -> "FAILED";
            case UNKNOWN, SUSPENDED_OPERATIONAL -> "UNKNOWN";
        };
        PaymentRefundExecutionAttempt attemptRecord = PaymentRefundExecutionAttempt.record(
                intent.getId(),
                nextAttempt,
                execResult.requestMethod(),
                auditStatus,
                execResult.httpStatus(),
                execResult.responseBody(),
                execResult.errorCode(),
                execResult.errorMessage(),
                now
        );
        executionAttemptRepository.save(attemptRecord);

        switch (execResult.status()) {
            case SUCCESSFUL -> {
                intent.complete(execResult.providerRefundId(), now);
                refundIntentRepository.save(intent);

                payment.completeRefund(execResult.providerRefundId(), now);
                paymentRepository.save(payment);

                try {
                    incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.REFUND_EXECUTION)
                            .ifPresent(inc -> {
                                inc.markResolved(now);
                                incidenceRepository.save(inc);
                            });
                } catch (Exception ex) {
                    log.error("Error al resolver incidencia de reembolso para pago {}: {}", paymentId, ex.getMessage());
                }
                log.info("Reembolso consolidado como COMPLETED para el pago {} con ID MP: {}",
                        paymentId, execResult.providerRefundId());
                return RefundConsolidationResult.completed(execResult.providerRefundId());
            }
            case FAILED -> {
                intent.fail(execResult.errorMessage(), now);
                refundIntentRepository.save(intent);

                payment.failRefund();
                paymentRepository.save(payment);

                try {
                    recordRefundIncidence(
                            paymentId,
                            IncidenceCategory.COMMERCIAL,
                            execResult.errorCode() != null ? execResult.errorCode() : "REFUND_FAILED",
                            execResult.errorMessage(),
                            IncidenceStatus.MANUAL_REVIEW_REQUIRED,
                            now
                    );
                } catch (Exception ex) {
                    log.error("Error al registrar incidencia para pago {}: {}", paymentId, ex.getMessage());
                }
                log.warn("Reembolso marcado como FAILED para el pago {}", paymentId);
                return RefundConsolidationResult.failed(execResult.errorMessage());
            }
            case SUSPENDED_OPERATIONAL -> {
                intent.markUnknown(execResult.errorMessage(), now);
                refundIntentRepository.save(intent);

                try {
                    recordRefundIncidence(
                            paymentId,
                            IncidenceCategory.OPERATIONAL,
                            execResult.errorCode() != null ? execResult.errorCode() : "MP_AUTH_OR_PERMISSION_ERROR",
                            execResult.errorMessage(),
                            IncidenceStatus.MANUAL_REVIEW_REQUIRED,
                            now
                    );
                } catch (Exception ex) {
                    log.error("Error al registrar incidencia operativa para pago {}: {}", paymentId, ex.getMessage());
                }
                log.warn("Reembolso suspendido por error operativo de credenciales/permisos para el pago {}. Escalar a revisión manual.", paymentId);
                return RefundConsolidationResult.suspended(execResult.errorMessage());
            }
            case UNKNOWN -> {
                intent.markUnknown(execResult.errorMessage(), now);
                refundIntentRepository.save(intent);

                try {
                    recordRefundTechnicalRetry(paymentId, execResult.errorCode(), execResult.errorMessage(), now);
                } catch (Exception ex) {
                    log.error("Error al registrar reintento técnico para pago {}: {}", paymentId, ex.getMessage());
                }
                log.warn("Resultado de reembolso indeterminado (UNKNOWN) para el pago {}. Reintento programado.", paymentId);
                return RefundConsolidationResult.indeterminate(execResult.errorMessage());
            }
        }

        return RefundConsolidationResult.error("Estado de resultado no soportado");
    }

    private void recordRefundIncidence(
            UUID paymentId,
            IncidenceCategory category,
            String code,
            String message,
            IncidenceStatus targetStatus,
            Instant now
    ) {
        Optional<PaymentIncidence> activeOpt = incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(
                paymentId, IncidenceProcessType.REFUND_EXECUTION
        );
        if (activeOpt.isPresent()) {
            PaymentIncidence inc = activeOpt.get();
            inc.recordRetry(Duration.ZERO, code, message, now);
            if (targetStatus == IncidenceStatus.MANUAL_REVIEW_REQUIRED) {
                inc.escalateToManualReview(message, now);
            }
            incidenceRepository.save(inc);
        } else {
            PaymentIncidence inc = new PaymentIncidence(
                    UUID.randomUUID(),
                    paymentId,
                    IncidenceProcessType.REFUND_EXECUTION,
                    category,
                    code,
                    message,
                    1,
                    5,
                    null,
                    targetStatus,
                    now,
                    now,
                    null
            );
            incidenceRepository.save(inc);
        }
    }

    private void recordRefundTechnicalRetry(UUID paymentId, String code, String message, Instant now) {
        Optional<PaymentIncidence> activeOpt = incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(
                paymentId, IncidenceProcessType.REFUND_EXECUTION
        );
        String finalCode = code != null ? code : "UNKNOWN";
        if (activeOpt.isPresent()) {
            PaymentIncidence inc = activeOpt.get();
            int nextAttempt = inc.getAttemptsCount() + 1;
            Duration backoff = PaymentIncidenceRecorder.calculateBackoff(nextAttempt);
            inc.recordRetry(backoff, finalCode, message, now);
            incidenceRepository.save(inc);
        } else {
            PaymentIncidence inc = PaymentIncidence.createNew(
                    paymentId,
                    IncidenceProcessType.REFUND_EXECUTION,
                    IncidenceCategory.TECHNICAL,
                    finalCode,
                    message,
                    PaymentIncidenceRecorder.calculateBackoff(1),
                    now
            );
            incidenceRepository.save(inc);
        }
    }

    private boolean isDefinitiveCommercialError(int httpStatus, String errorCode, String message) {
        if (httpStatus == 404) {
            return true;
        }
        if (httpStatus == 400) {
            if (errorCode == null && message == null) {
                return false;
            }
            String combined = ((errorCode != null ? errorCode : "") + " " + (message != null ? message : "")).toLowerCase(Locale.ROOT);
            return combined.contains("refund_amount_exceeded")
                    || combined.contains("payment_not_refundable")
                    || combined.contains("payment_cannot_be_refunded")
                    || combined.contains("bad_request_refund_not_allowed")
                    || combined.contains("cannot_refund")
                    || combined.contains("charged_back")
                    || combined.contains("refund_expired")
                    || combined.contains("already_refunded")
                    || combined.contains("too old");
        }
        return false;
    }

    private boolean isOperationalAuthOrPermissionError(int httpStatus) {
        return httpStatus == 401 || httpStatus == 403;
    }

    public record RefundClaimResult(
            boolean claimed,
            String skipReason,
            UUID paymentId,
            UUID intentId,
            UUID claimToken,
            String checkoutMode,
            String providerOrderId,
            String providerPaymentId,
            BigDecimal amount,
            String idempotencyKey,
            RefundIntentStatus previousStatus
    ) {
        public static RefundClaimResult claimed(
                UUID paymentId, UUID intentId, UUID claimToken, String checkoutMode,
                String providerOrderId, String providerPaymentId,
                BigDecimal amount, String idempotencyKey, RefundIntentStatus previousStatus
        ) {
            return new RefundClaimResult(
                    true, null, paymentId, intentId, claimToken,
                    checkoutMode != null && !checkoutMode.isBlank() ? checkoutMode : "PREFERENCES",
                    providerOrderId, providerPaymentId, amount, idempotencyKey, previousStatus
            );
        }

        public static RefundClaimResult claimed(
                UUID paymentId, UUID intentId, UUID claimToken, String providerPaymentId,
                BigDecimal amount, String idempotencyKey, RefundIntentStatus previousStatus
        ) {
            return claimed(paymentId, intentId, claimToken, "PREFERENCES", null, providerPaymentId, amount, idempotencyKey, previousStatus);
        }

        public static RefundClaimResult skipped(String reason) {
            return new RefundClaimResult(false, reason, null, null, null, "PREFERENCES", null, null, null, null, null);
        }
    }

    public record RefundExecutionResult(
            ExecutionStatus status,
            String requestMethod,
            Integer httpStatus,
            String providerRefundId,
            BigDecimal amount,
            String responseBody,
            String errorCode,
            String errorMessage
    ) {
        public enum ExecutionStatus { SUCCESSFUL, FAILED, UNKNOWN, SUSPENDED_OPERATIONAL }

        public static RefundExecutionResult success(String method, int httpStatus, String refundId, BigDecimal amount, String body, String msg) {
            return new RefundExecutionResult(ExecutionStatus.SUCCESSFUL, method, httpStatus, refundId, amount, body, null, msg);
        }

        public static RefundExecutionResult definitiveFailure(String method, int httpStatus, String code, String msg) {
            return new RefundExecutionResult(ExecutionStatus.FAILED, method, httpStatus, null, null, null, code, msg);
        }

        public static RefundExecutionResult operationalSuspension(String method, int httpStatus, String code, String msg) {
            return new RefundExecutionResult(ExecutionStatus.SUSPENDED_OPERATIONAL, method, httpStatus, null, null, null, code, msg);
        }

        public static RefundExecutionResult indeterminate(String method, int httpStatus, String code, String msg) {
            return new RefundExecutionResult(ExecutionStatus.UNKNOWN, method, httpStatus, null, null, null, code, msg);
        }
    }

    public record RefundConsolidationResult(
            ConsolidationStatus status,
            String message,
            String providerRefundId
    ) {
        public enum ConsolidationStatus { COMPLETED, FAILED, INDETERMINATE, CLAIM_LOST, SUSPENDED, ERROR }

        public static RefundConsolidationResult completed(String refundId) {
            return new RefundConsolidationResult(ConsolidationStatus.COMPLETED, "Reembolso completado", refundId);
        }
        public static RefundConsolidationResult failed(String msg) {
            return new RefundConsolidationResult(ConsolidationStatus.FAILED, msg, null);
        }
        public static RefundConsolidationResult suspended(String msg) {
            return new RefundConsolidationResult(ConsolidationStatus.SUSPENDED, msg, null);
        }
        public static RefundConsolidationResult indeterminate(String msg) {
            return new RefundConsolidationResult(ConsolidationStatus.INDETERMINATE, msg, null);
        }
        public static RefundConsolidationResult claimLost(String msg) {
            return new RefundConsolidationResult(ConsolidationStatus.CLAIM_LOST, msg, null);
        }
        public static RefundConsolidationResult error(String msg) {
            return new RefundConsolidationResult(ConsolidationStatus.ERROR, msg, null);
        }
    }
}
