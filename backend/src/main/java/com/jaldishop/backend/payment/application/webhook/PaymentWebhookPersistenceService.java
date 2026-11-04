package com.jaldishop.backend.payment.application.webhook;

import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Servicio transaccional que aplica los resultados verificados de la consulta
 * al proveedor sobre los agregados Payment y PaymentAttempt, asegurando idempotencia
 * y protección estricta contra eventos fuera de orden.
 */
@Service
public class PaymentWebhookPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookPersistenceService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    public PaymentWebhookPersistenceService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    @Transactional
    public ProcessMercadoPagoWebhookResult applyQueryResultToPayment(UUID paymentId, PaymentQueryResult queryResult) {
        // Bloqueo pesimista PESSIMISTIC_WRITE sobre Payment para serializar webhooks concurrentes
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado para ID: " + paymentId));

        // 1. Verificación de Invariantes Financieras: Monto y Moneda
        if (payment.getAmount().compareTo(queryResult.transactionAmount()) != 0) {
            log.error("Discrepancia financiera: el monto pagado ({}) difiere del monto registrado ({}) para el pago {}",
                    queryResult.transactionAmount(), payment.getAmount(), paymentId);
            throw new ConflictException("PAYMENT_AMOUNT_MISMATCH",
                    String.format("El monto notificado (%s) no coincide con el monto esperado (%s)",
                            queryResult.transactionAmount(), payment.getAmount()));
        }

        if (!payment.getCurrency().equalsIgnoreCase(queryResult.currency())) {
            log.error("Discrepancia de divisa: moneda pagada ({}) difiere de la esperada ({}) para el pago {}",
                    queryResult.currency(), payment.getCurrency(), paymentId);
            throw new ConflictException("PAYMENT_CURRENCY_MISMATCH",
                    String.format("La moneda notificada (%s) no coincide con la moneda esperada (%s)",
                            queryResult.currency(), payment.getCurrency()));
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId);
        PaymentAttempt targetAttempt = null;
        for (PaymentAttempt a : attempts) {
            if (queryResult.providerPaymentId().equals(a.getProviderPaymentId())) {
                targetAttempt = a;
                break;
            }
        }
        if (targetAttempt == null && !attempts.isEmpty()) {
            targetAttempt = attempts.getLast();
        }

        String mpStatus = queryResult.status().toLowerCase(Locale.ROOT);
        PaymentMethod resolvedMethod = resolvePaymentMethod(queryResult.paymentTypeId(), queryResult.paymentMethodId());

        // 2. Protección de Estados Terminales y Eventos Fuera de Orden:
        // Si el pago ya fue aprobado, un evento tardío o repetido de estado inferior (ej. pending) no debe degradarlo.
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("Pago {} ya se encuentra en estado APPROVED. Ignorando actualización extemporánea a {}.",
                    paymentId, mpStatus);
            String attemptStatus = targetAttempt != null ? targetAttempt.getStatus().name() : "NONE";
            return ProcessMercadoPagoWebhookResult.success(
                    paymentId, queryResult.providerPaymentId(), payment.getStatus().name(), attemptStatus,
                    "Pago previamente aprobado; idempotencia aplicada sin degradación de estado."
            );
        }

        // 3. Transición según estado oficial del proveedor
        switch (mpStatus) {
            case "approved" -> {
                payment.markApproved(queryResult.dateApproved());
                paymentRepository.save(payment);

                if (targetAttempt != null) {
                    targetAttempt.markApproved(
                            queryResult.providerPaymentId(),
                            queryResult.status(),
                            queryResult.statusDetail(),
                            resolvedMethod,
                            queryResult.dateApproved()
                    );
                    paymentAttemptRepository.save(targetAttempt);
                }
                log.info("Pago {} y su intento {} actualizados a APPROVED conforme a Mercado Pago.",
                        paymentId, targetAttempt != null ? targetAttempt.getId() : "N/A");
            }
            case "rejected", "cancelled" -> {
                if (targetAttempt != null) {
                    targetAttempt.markRejected(
                            queryResult.providerPaymentId(),
                            queryResult.status(),
                            queryResult.statusDetail(),
                            queryResult.status(),
                            queryResult.statusDetail(),
                            Instant.now()
                    );
                    paymentAttemptRepository.save(targetAttempt);
                }
                log.info("Intento de pago {} actualizado a REJECTED por Mercado Pago. El pago principal se mantiene en {} para reintento.",
                        targetAttempt != null ? targetAttempt.getId() : "N/A", payment.getStatus());
            }
            case "pending", "in_process", "authorized" -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.markProcessing();
                    paymentRepository.save(payment);
                }
                if (targetAttempt != null) {
                    targetAttempt.markProviderPending(
                            queryResult.providerPaymentId(),
                            queryResult.status(),
                            queryResult.statusDetail(),
                            resolvedMethod
                    );
                    paymentAttemptRepository.save(targetAttempt);
                }
                log.info("Pago {} e intento en estado intermedio: {}.", paymentId, mpStatus);
            }
            default -> {
                log.warn("Estado desconocido de Mercado Pago ({}) recibido para pago {}.", mpStatus, paymentId);
            }
        }

        String attemptStatus = targetAttempt != null ? targetAttempt.getStatus().name() : "NONE";
        return ProcessMercadoPagoWebhookResult.success(
                paymentId, queryResult.providerPaymentId(), payment.getStatus().name(), attemptStatus,
                "Estado del pago actualizado conforme a consulta con Mercado Pago."
        );
    }

    /**
     * Registra una incidencia cuando un pago fue aprobado por Mercado Pago pero la orden comercial
     * no pudo ser confirmada (ej. expiración extemporánea de la reserva de capacidad o inventario insuficiente).
     * Mantiene el Payment en estado APPROVED y marca refundStatus = PENDING para futura conciliación/reembolso,
     * ejecutándose en una transacción independiente REQUIRES_NEW.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordApprovalIncidence(UUID paymentId, String reason) {
        try {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
            if (payment == null) {
                return;
            }

            String truncatedReason = reason != null && reason.length() > 80 ? reason.substring(0, 80) : (reason != null ? reason : "INCIDENCE");
            payment.requestRefund(payment.getAmount(), "INCIDENCE:" + truncatedReason);
            paymentRepository.save(payment);

            List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId);
            if (!attempts.isEmpty()) {
                PaymentAttempt targetAttempt = attempts.getLast();
                targetAttempt.recordIncidence("COMMERCIAL_CONFIRMATION_FAILED", reason);
                paymentAttemptRepository.save(targetAttempt);
            }

            log.info("Incidencia comercial registrada para pago APPROVED {}: {}", paymentId, reason);
        } catch (Exception e) {
            log.error("Error al registrar incidencia para pago {}: {}", paymentId, e.getMessage(), e);
        }
    }

    private PaymentMethod resolvePaymentMethod(String paymentTypeId, String paymentMethodId) {
        if (paymentMethodId != null && paymentMethodId.equalsIgnoreCase("yape")) {
            return PaymentMethod.YAPE;
        }
        if (paymentTypeId == null) {
            return PaymentMethod.CARD;
        }
        return switch (paymentTypeId.toLowerCase(Locale.ROOT)) {
            case "credit_card", "debit_card", "prepaid_card" -> PaymentMethod.CARD;
            case "account_money" -> PaymentMethod.ACCOUNT_MONEY;
            case "bank_transfer" -> PaymentMethod.BANK_TRANSFER;
            case "ticket" -> PaymentMethod.TICKET;
            case "wallet_purchase" -> PaymentMethod.WALLET_PURCHASE;
            default -> PaymentMethod.OTHER;
        };
    }

}
