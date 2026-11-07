package com.jaldishop.backend.payment.application.reconciliation;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidator;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Procesador de conciliación activa para órdenes Orders API en 3 fases:
 * 1. claimAttempt: selección y lease transaccional corto.
 * 2. queryRemoteOrder: consulta HTTP fuera de transacción de BD (sin bloqueos pesimistas).
 * 3. consolidateOrderReconciliation:
 *    - 3A. applyFinancialReconciliationUnderLock: validación de lease, aplicación financiera y liberación de lease bajo transacción corta (REQUIRES_NEW).
 *    - 3B. confirmCommercial: reutiliza la confirmación comercial y sus incidencias en transacciones independientes.
 */
@Component
public class PaymentOrderReconciliationProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderReconciliationProcessor.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MercadoPagoGateway mercadoPagoGateway;
    private final PaymentWebhookPersistenceService persistenceService;
    private final PaymentReconciliationProcessor commercialProcessor;
    private final MercadoPagoResourceValidator resourceValidator;
    private final PaymentIncidenceRepository incidenceRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @Autowired
    public PaymentOrderReconciliationProcessor(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            MercadoPagoGateway mercadoPagoGateway,
            PaymentWebhookPersistenceService persistenceService,
            PaymentReconciliationProcessor commercialProcessor,
            OrderRepository orderRepository,
            PlatformTransactionManager transactionManager,
            MercadoPagoResourceValidator resourceValidator,
            PaymentIncidenceRepository incidenceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.mercadoPagoGateway = mercadoPagoGateway;
        this.persistenceService = persistenceService;
        this.commercialProcessor = commercialProcessor;
        this.resourceValidator = resourceValidator;
        this.incidenceRepository = incidenceRepository;
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
        // Route before REQUIRES_NEW so its persistence context reloads the attempt only under lock.
        PaymentAttempt initial = paymentAttemptRepository.findById(attemptId).orElse(null);
        if (initial == null) return OrderReconciliationClaimResult.skipped("Intento inexistente");
        return requiresNewTransactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(initial.getPaymentId()).orElse(null);
            PaymentAttempt attempt = paymentAttemptRepository.findByIdForUpdate(attemptId).orElse(null);
            if (attempt == null || !initial.getPaymentId().equals(attempt.getPaymentId())) {
                return OrderReconciliationClaimResult.skipped("Intento inexistente: " + attemptId);
            }

            Instant claimTime = Instant.now();
            if (attempt.getProvider() != PaymentProvider.MERCADO_PAGO
                    || leaseDuration == null || leaseDuration.isNegative() || leaseDuration.isZero()
                    || !attempt.canClaimReconciliation(claimTime)) {
                return OrderReconciliationClaimResult.skipped("Intento no elegible o con lease activo de otro worker");
            }

            if (payment == null) {
                return OrderReconciliationClaimResult.skipped("Pago inexistente para intento: " + attemptId);
            }

            if (payment.getStatus() == PaymentStatus.APPROVED) {
                return OrderReconciliationClaimResult.skipped("El pago ya se encuentra en APPROVED");
            }

            attempt.acquireReconciliationLease(workerToken, leaseDuration, claimTime);
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
                applyFinancialReconciliationUnderLock(claim, remoteOrder, workerToken)
        );

        if (outcome == null) {
            return OrderReconciliationCandidateResult.technicalFailure(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), "Error en transacción de consolidación");
        }

        return switch (outcome.status()) {
            case CLAIM_LOST -> OrderReconciliationCandidateResult.claimLost(
                    claim.attemptId(), claim.paymentId(), outcome.message());
            case ALREADY_APPROVED -> confirmCommercial(claim.attemptId(), claim.paymentId(), claim.providerOrderId());
            case TECHNICAL_FAILURE -> OrderReconciliationCandidateResult.technicalFailure(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), outcome.message());
            case ORDER_OPEN -> OrderReconciliationCandidateResult.orderOpen(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId());
            case CANCELLED_OR_EXPIRED -> OrderReconciliationCandidateResult.cancelledOrExpired(
                    claim.attemptId(), claim.paymentId(), claim.providerOrderId(), outcome.paymentStatus());
            case FINANCIAL_APPLIED -> "APPROVED".equalsIgnoreCase(outcome.paymentStatus())
                    ? confirmCommercial(claim.attemptId(), claim.paymentId(), claim.providerOrderId())
                    : OrderReconciliationCandidateResult.orderOpen(claim.attemptId(), claim.paymentId(), claim.providerOrderId());
        };
    }

    /**
     * Fase 3A: Transacción corta que revalida el lease, aplica la transición de estado financiero y libera el lease.
     */
    private FinancialReconciliationOutcome applyFinancialReconciliationUnderLock(
            OrderReconciliationClaimResult claim,
            OrderQueryResult remoteOrder,
            UUID workerToken
    ) {
        Payment payment = paymentRepository.findByIdForUpdate(claim.paymentId()).orElse(null);
        PaymentAttempt attempt = paymentAttemptRepository.findByIdForUpdate(claim.attemptId()).orElse(null);
        Instant now = Instant.now();
        if (attempt == null) {
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CLAIM_LOST, null, null, "Intento no encontrado en BD");
        }

        if (!attempt.isReconciliationClaimOwnedBy(workerToken, now)) {
            log.warn("CLAIM OBSOLETO: Worker {} no es dueño del lease vigente para intento {}. Consolidación rechazada.",
                    workerToken, claim.attemptId());
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CLAIM_LOST, null, null, "Lease expirado o reclamado por otro worker");
        }

        if (payment == null) {
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.TECHNICAL_FAILURE, null, null, "Pago no encontrado");
        }

        resourceValidator.validateOrder(claim.providerOrderId(), remoteOrder);
        if (!claim.paymentId().equals(attempt.getPaymentId())
                || !claim.providerOrderId().equals(attempt.getProviderOrderId()))
            throw new ConflictException("PAYMENT_ATTEMPT_MISMATCH", "IDENTIFIER_CONTRADICTION");
        persistenceService.validateReference(remoteOrder.externalReference(), payment.getId(), attempt);

        // Si el pago ya fue aprobado concurrentemente (ej. por webhook tardío), no degradar
        if (payment.getStatus() == PaymentStatus.APPROVED && remoteOrder.payments().isEmpty()) {
            log.info("Pago {} ya se encuentra en APPROVED (notificación concurrente). Liberando lease.", payment.getId());
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.ALREADY_APPROVED, "APPROVED", null, "Ya aprobado");
        }

        // Caso A: Orden remota sin transacciones de pago
        if (remoteOrder.payments().isEmpty()) {
            String remoteStatus = remoteOrder.status() != null ? remoteOrder.status().toLowerCase(Locale.ROOT) : "";
            if ("canceled".equals(remoteStatus) || "cancelled".equals(remoteStatus) || "expired".equals(remoteStatus)) {
                attempt.releaseReconciliationLease(now);
                paymentAttemptRepository.save(attempt);
                persistenceService.applyOrderCancellation(payment.getId(), claim.providerOrderId(), remoteOrder.status(), remoteOrder.statusDetail(), remoteOrder.externalReference());
                log.info("Orden {} cancelada/expirada remotamente sin transacciones. Intento marcado como REJECTED.", claim.providerOrderId());
                return new FinancialReconciliationOutcome(FinancialReconciliationStatus.CANCELLED_OR_EXPIRED, remoteOrder.status(), null, "Orden cancelada/expirada");
            }

            // Orden created / abierta: permanece abierta esperando pago
            attempt.releaseReconciliationLease(now);
            paymentAttemptRepository.save(attempt);
            log.info("Orden {} permanece abierta en Mercado Pago ({}) sin transacciones. Estado local preservado.", claim.providerOrderId(), remoteOrder.status());
            return new FinancialReconciliationOutcome(FinancialReconciliationStatus.ORDER_OPEN, remoteOrder.status(), null, "Orden abierta sin pagos");
        }

        // Single transaction identity was checked by the shared validator.
        var selectedPayment = remoteOrder.payments().getFirst();

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

        if (!"APPROVED".equalsIgnoreCase(persistResult.paymentStatus())
                && "REJECTED".equals(persistResult.attemptStatus())) {
            return new FinancialReconciliationOutcome(
                    FinancialReconciliationStatus.CANCELLED_OR_EXPIRED,
                    selectedPayment.status(), selectedPayment.providerPaymentId(), "Intento rechazado o cancelado");
        }

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
        requiresNewTransactionTemplate.executeWithoutResult(status ->
                paymentAttemptRepository.findByIdForUpdate(attemptId).ifPresent(attempt -> {
                    if (workerToken.equals(attempt.getReconciliationClaimToken())) {
                        attempt.releaseReconciliationLease(Instant.now());
                        paymentAttemptRepository.save(attempt);
                    }
                }));
    }

    /** Returns null only when a remote financial reconciliation is still needed. */
    public OrderReconciliationCandidateResult reconcileApprovedAttempt(UUID attemptId) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) return OrderReconciliationCandidateResult.skipped(attemptId, "Intento inexistente");
        if (attempt.getProvider() != PaymentProvider.MERCADO_PAGO
                || !"ORDERS".equals(attempt.getCheckoutMode()) || attempt.getProviderOrderId() == null
                || attempt.getProviderOrderId().isBlank())
            return OrderReconciliationCandidateResult.skipped(attemptId, "Se requiere un intento Mercado Pago Orders");
        Payment payment = paymentRepository.findById(attempt.getPaymentId()).orElseThrow();
        return payment.getStatus() == PaymentStatus.APPROVED
                ? confirmCommercial(attemptId, payment.getId(), attempt.getProviderOrderId()) : null;
    }

    private OrderReconciliationCandidateResult confirmCommercial(UUID attemptId, UUID paymentId, String orderId) {
        var existing = orderRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            return OrderReconciliationCandidateResult.successConfirmed(attemptId, paymentId, orderId,
                    existing.get().getOrderNumber());
        }
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        if (payment.getRefundStatus() != RefundStatus.NOT_REQUIRED) {
            var incidence = incidenceRepository.findLatestByPaymentIdAndProcessType(paymentId,
                    IncidenceProcessType.ORDER_CONFIRMATION);
            return incidence.filter(i -> i.getCategory() == com.jaldishop.backend.payment.domain.enums.IncidenceCategory.COMMERCIAL).isPresent()
                    ? OrderReconciliationCandidateResult.commercialIncidence(attemptId, paymentId, orderId, incidence.get().getCode())
                    : OrderReconciliationCandidateResult.commercialPending(attemptId, paymentId, orderId);
        }
        ReconciliationCandidateResult result = commercialProcessor.processCandidate(paymentId);
        return switch (result.status()) {
            case SUCCESS -> OrderReconciliationCandidateResult.successConfirmed(attemptId, paymentId, orderId, result.orderNumber());
            case COMMERCIAL_FAILURE -> OrderReconciliationCandidateResult.commercialIncidence(attemptId, paymentId, orderId, result.detail());
            case SKIPPED, INDETERMINATE_CONFLICT -> OrderReconciliationCandidateResult.commercialPending(attemptId, paymentId, orderId);
            case TRANSIENT_FAILURE, UNEXPECTED_FAILURE -> OrderReconciliationCandidateResult.technicalFailure(attemptId, paymentId, orderId,
                    "Fallo técnico en confirmación comercial; aprobación financiera preservada");
        };
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
