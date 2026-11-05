package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.webhook.PaymentWebhookPersistenceService;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookResult;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Procesador de conciliación activa para órdenes Orders API en 3 fases:
 * 1. claimAttempt: selección y lease transaccional corto.
 * 2. queryRemoteOrder: consulta HTTP fuera de transacción de BD (sin bloqueos pesimistas).
 * 3. consolidateOrderReconciliation:
 *    - 3A. applyFinancialReconciliationUnderLock: validación de lease, aplicación financiera y liberación de lease bajo transacción corta (REQUIRES_NEW).
 *    - 3B. confirmPurchaseIfApproved: confirmación comercial fuera de bloqueos de pagos, registrando incidencias si la reserva expiró.
 */
@Component
public class PaymentOrderReconciliationProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderReconciliationProcessor.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PaymentWebhookPersistenceService persistenceService;
    private final ConfirmPurchaseService confirmPurchaseService;
    private final OrderRepository orderRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @Autowired
    public PaymentOrderReconciliationProcessor(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            MercadoPagoGateway mercadoPagoGateway,
            PaymentWebhookPersistenceService persistenceService,
            ConfirmPurchaseService confirmPurchaseService,
            OrderRepository orderRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.persistenceService = persistenceService;
        this.confirmPurchaseService = confirmPurchaseService;
        this.orderRepository = orderRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Fase 1: Reclama el intento de pago mediante un lease temporal bajo transacción corta.
     */
    public OrderReconciliationClaimResult claimAttempt(
            UUID attemptId,
            UUID workerToken,
            Duration leaseDuration,
            Instant now
    ) {
        return requiresNewTransactionTemplate.execute(status -> {
            PaymentAttempt attempt = paymentAttemptRepository.findByIdForUpdate(attemptId).orElse(null);
            if (attempt == null) {
                return OrderReconciliationClaimResult.skipped("Intento inexistente: " + attemptId);
            }

            if (!attempt.canClaimReconciliation(now)) {
                return OrderReconciliationClaimResult.skipped("Intento no elegible o con lease activo de otro worker");
            }

            Payment payment = paymentRepository.findById(attempt.getPaymentId()).orElse(null);
            if (payment == null) {
                return OrderReconciliationClaimResult.skipped("Pago inexistente para intento: " + attemptId);
            }

            if (payment.getStatus() == PaymentStatus.APPROVED) {
                return OrderReconciliationClaimResult.skipped("El pago ya se encuentra en APPROVED");
            }

            attempt.acquireReconciliationLease(workerToken, leaseDuration, now);
            paymentAttemptRepository.save(attempt);

            log.info("Lease de conciliación adquirido para intento {} (pago {}) por worker {} hasta {}",
                    attempt.getId(), payment.getId(), workerToken, attempt.getReconciliationLeaseExpiresAt());

            return OrderReconciliationClaimResult.claimed(
                    attempt.getId(),
                    payment.getId(),
                    workerToken,
                    attempt.getProviderOrderId()
            );
        });
    }

    /**
     * Fase 2: Consulta remota en Mercado Pago fuera de cualquier transacción de PostgreSQL.
     */
    public OrderQueryResult queryRemoteOrder(String providerOrderId) {
        log.info("Consultando estado de orden remota en Mercado Pago: {}", providerOrderId);
        return mercadoPagoGateway.queryOrder(providerOrderId);
    }

    /**
     * Fase 3: Consolidación.
     * Ejecuta la aplicación financiera bajo lock y posteriormente la confirmación comercial fuera del lock de pagos.
     */
    public OrderReconciliationCandidateResult consolidateOrderReconciliation(
            OrderReconciliationClaimResult claim,
            OrderQueryResult remoteOrder,
            UUID workerToken,
            Instant now
    ) {
        FinancialReconciliationOutcome outcome = requiresNewTransactionTemplate.execute(status ->
                applyFinancialReconciliationUnderLock(claim, remoteOrder, workerToken, now)
        );

        if (outcome == null) {
            return OrderReconciliationCandidateResult.technicalFailure(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), "Error en transacción de consolidación");
        }

        return switch (outcome.status()) {
            case CLAIM_LOST -> OrderReconciliationCandidateResult.claimLost(
                    claim.attemptId(), claim.paymentId(), outcome.message());
            case ALREADY_APPROVED -> OrderReconciliationCandidateResult.alreadyApproved(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId());
            case TECHNICAL_FAILURE -> OrderReconciliationCandidateResult.technicalFailure(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), outcome.message());
            case ORDER_OPEN -> OrderReconciliationCandidateResult.orderOpen(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId());
            case CANCELLED_OR_EXPIRED -> OrderReconciliationCandidateResult.cancelledOrExpired(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), outcome.paymentStatus());
            case FINANCIAL_APPLIED -> {
                if ("APPROVED".equalsIgnoreCase(outcome.paymentStatus()) && confirmPurchaseService != null) {
                    try {
                        OrderConfirmation conf = confirmPurchaseService.execute(new ConfirmPurchaseCommand(claim.paymentId()));
                        log.info("Conciliación exitosa: pedido {} confirmado comercialmente para pago {}", conf.orderNumber(), claim.paymentId());
                        yield OrderReconciliationCandidateResult.successConfirmed(
                                claim.attemptId(), claim.paymentId(), claim.providerOrderId(), conf.orderNumber());
                    } catch (ConflictException ex) {
                        log.warn("Fallo comercial definitivo en confirmación durante conciliación para pago APPROVED {}: [{}] {}",
                                claim.paymentId(), ex.getCode(), ex.getMessage());
                        persistenceService.recordApprovalIncidence(claim.paymentId(), ex.getMessage());
                        yield OrderReconciliationCandidateResult.commercialIncidence(
                                claim.attemptId(), claim.paymentId(), claim.providerOrderId(), ex.getMessage());
                    } catch (DataIntegrityViolationException ex) {
                        log.warn("Violación de integridad al confirmar pedido para pago {} en conciliación. Verificando ganador...", claim.paymentId());
                        if (orderRepository != null) {
                            Optional<Order> existingOrder = orderRepository.findByPaymentId(claim.paymentId());
                            if (existingOrder.isPresent()) {
                                yield OrderReconciliationCandidateResult.successConfirmed(
                                        claim.attemptId(), claim.paymentId(), claim.providerOrderId(), existingOrder.get().getOrderNumber());
                            }
                        }
                        throw ex;
                    }
                }
                yield OrderReconciliationCandidateResult.cancelledOrExpired(
                        claim.attemptId(), claim.paymentId(), claim.providerOrderId(), outcome.paymentStatus());
            }
        };
    }

    /**
     * Fase 3A: Transacción corta que revalida el lease, aplica la transición de estado financiero y libera el lease.
     */
    private FinancialReconciliationOutcome applyFinancialReconciliationUnderLock(
            OrderReconciliationClaimResult claim,
            OrderQueryResult remoteOrder,
            UUID workerToken,
            Instant now
    ) {
        PaymentAttempt attempt = paymentAttemptRepository.findByIdForUpdate(claim.attemptId()).orElse(null);
        if (attempt == null) {
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CLAIM_LOST, null, null, "Intento no encontrado en BD");
        }

        if (!attempt.isReconciliationClaimOwnedBy(workerToken, now)) {
            log.warn("CLAIM OBSOLETO: Worker {} no es dueño del lease vigente para intento {}. Consolidación rechazada.",
                    workerToken, claim.attemptId());
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CLAIM_LOST, null, null, "Lease expirado o reclamado por otro worker");
        }

        Payment payment = paymentRepository.findByIdForUpdate(claim.paymentId()).orElse(null);
        if (payment == null) {
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.TECHNICAL_FAILURE, null, null, "Pago no encontrado");
        }

        // Si el pago ya fue aprobado concurrentemente (ej. por webhook tardío), no degradar
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("Pago {} ya se encuentra en APPROVED (notificación concurrente). Liberando lease.", payment.getId());
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.ALREADY_APPROVED, "APPROVED", null, "Ya aprobado");
        }

        if (remoteOrder == null) {
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.TECHNICAL_FAILURE, null, null, "Respuesta remota nula");
        }

        // Caso A: Orden remota sin transacciones de pago
        if (remoteOrder.payments().isEmpty()) {
            String remoteStatus = remoteOrder.status() != null ? remoteOrder.status().toLowerCase(Locale.ROOT) : "";
            if ("cancelled".equals(remoteStatus) || "expired".equals(remoteStatus)) {
                attempt.releaseReconciliationLease(now);
                paymentAttemptRepository.save(attempt);
                persistenceService.applyOrderCancellation(payment.getId(), claim.providerOrderId(), remoteOrder.status(), remoteOrder.statusDetail());
                log.info("Orden {} cancelada/expirada remotamente sin transacciones. Intento marcado como REJECTED.", claim.providerOrderId());
                return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CANCELLED_OR_EXPIRED, remoteOrder.status(), null, "Orden cancelada/expirada");
            }

            // Orden created / abierta: permanece abierta esperando pago
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            log.info("Orden {} permanece abierta en Mercado Pago ({}) sin transacciones. Estado local preservado.", claim.providerOrderId(), remoteOrder.status());
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.ORDER_OPEN, remoteOrder.status(), null, "Orden abierta sin pagos");
        }

        // Caso B: Orden remota contiene transacciones
        OrderQueryResult.OrderPaymentItem selectedPayment = null;
        for (OrderQueryResult.OrderPaymentItem p : remoteOrder.payments()) {
            if (isPaymentAccredited(p)) {
                selectedPayment = p;
                break;
            }
        }
        if (selectedPayment == null) {
            selectedPayment = remoteOrder.payments().getLast();
        }

        PaymentQueryResult pqr = new PaymentQueryResult(
                selectedPayment.providerPaymentId(),
                selectedPayment.status(),
                selectedPayment.statusDetail(),
                selectedPayment.transactionAmount(),
                selectedPayment.currency(),
                selectedPayment.paymentMethodId(),
                selectedPayment.paymentTypeId(),
                remoteOrder.externalReference(),
                selectedPayment.dateApproved(),
                remoteOrder.collectorId(),
                remoteOrder.liveMode()
        );

        attempt.releaseReconciliationLease(now);
        paymentAttemptRepository.save(attempt);
        ProcessMercadoPagoWebhookResult persistResult = persistenceService.applyPaymentQueryResult(payment.getId(), pqr, claim.providerOrderId());

        return new FinancialReconciliationOutcome(
                FinancialReconciliationStatus.FINANCIAL_APPLIED,
                persistResult.paymentStatus(),
                selectedPayment.providerPaymentId(),
                "Transición financiera aplicada exitosamente"
        );
    }

    /**
     * Libera el lease en caso de error técnico en la Fase 2 (timeout, fallo HTTP) sin degradar estado.
     */
    public void releaseLeaseOnError(UUID attemptId, UUID workerToken, Instant now) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> {
            try {
                paymentAttemptRepository.findByIdForUpdate(attemptId).ifPresent(attempt -> {
                    if (attempt.isReconciliationClaimOwnedBy(workerToken, now)) {
                        attempt.releaseReconciliationLease(now);
                        paymentAttemptRepository.save(attempt);
                        log.info("Lease liberado tras error técnico para intento {}", attemptId);
                    }
                });
            } catch (Exception e) {
                log.error("Error liberando lease para intento {}: {}", attemptId, e.getMessage());
            }
        });
    }

    private boolean isPaymentAccredited(OrderQueryResult.OrderPaymentItem p) {
        if (p == null || p.providerPaymentId() == null || p.providerPaymentId().isBlank()) {
            return false;
        }
        String s = p.status() != null ? p.status().toLowerCase(Locale.ROOT) : "";
        String d = p.statusDetail() != null ? p.statusDetail().toLowerCase(Locale.ROOT) : "";
        if ("approved".equals(s)) {
            return d.isEmpty() || "accredited".equals(d);
        }
        if ("processed".equals(s)) {
            return "accredited".equals(d);
        }
        return false;
    }

    public enum FinancialReconciliationStatus {
        CLAIM_LOST,
        ALREADY_APPROVED,
        CANCELLED_OR_EXPIRED,
        ORDER_OPEN,
        FINANCIAL_APPLIED,
        TECHNICAL_FAILURE
    }

    public record FinancialReconciliationOutcome(
            FinancialReconciliationStatus status,
            String paymentStatus,
            String providerPaymentId,
            String message
    ) {}
}
