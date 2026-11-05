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
        return applyPaymentQueryResult(paymentId, queryResult, null);
    }

    @Transactional
    public ProcessMercadoPagoWebhookResult applyPaymentQueryResult(
            UUID paymentId,
            PaymentQueryResult queryResult,
            String providerOrderId
    ) {
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

        // Búsqueda de intento objetivo estrictamente inequívoco con evidencia externa:
        // 1. Coincidencia directa por identificador de transacción financiera de Mercado Pago
        for (PaymentAttempt a : attempts) {
            if (queryResult.providerPaymentId().equals(a.getProviderPaymentId())) {
                targetAttempt = a;
                break;
            }
        }

        // 2. Coincidencia directa por identificador de orden externa (Orders API)
        if (targetAttempt == null && providerOrderId != null && !providerOrderId.isBlank()) {
            for (PaymentAttempt a : attempts) {
                if (providerOrderId.equals(a.getProviderOrderId())) {
                    targetAttempt = a;
                    break;
                }
            }
            if (targetAttempt == null) {
                for (int i = attempts.size() - 1; i >= 0; i--) {
                    PaymentAttempt a = attempts.get(i);
                    if ("ORDERS".equalsIgnoreCase(a.getCheckoutMode())
                            && (a.getStatus() == PaymentAttemptStatus.PROCESSING || a.getStatus() == PaymentAttemptStatus.CREATED)
                            && (a.getProviderOrderId() == null || providerOrderId.equals(a.getProviderOrderId()))) {
                        targetAttempt = a;
                        break;
                    }
                }
            }
        }

        // 3. Flujo Preferences API: Mercado Pago notifica topic=payment con providerPaymentId recién generado.
        // Si no tenemos providerOrderId pero el intento activo fue registrado bajo modalidad PREFERENCES,
        // vinculamos el pago a dicho intento.
        if (targetAttempt == null && (providerOrderId == null || providerOrderId.isBlank())) {
            for (int i = attempts.size() - 1; i >= 0; i--) {
                PaymentAttempt a = attempts.get(i);
                if ("PREFERENCES".equalsIgnoreCase(a.getCheckoutMode())
                        && (a.getStatus() == PaymentAttemptStatus.PROCESSING || a.getStatus() == PaymentAttemptStatus.CREATED)
                        && (a.getProviderPaymentId() == null || a.getProviderPaymentId().equals(queryResult.providerPaymentId()))) {
                    targetAttempt = a;
                    break;
                }
            }
        }

        // 4. Fallback seguro: Si existe un único intento en progreso para el pago identificado, vincularlo
        if (targetAttempt == null && attempts.size() == 1) {
            PaymentAttempt soleAttempt = attempts.get(0);
            if (soleAttempt.getStatus() == PaymentAttemptStatus.PROCESSING || soleAttempt.getStatus() == PaymentAttemptStatus.CREATED) {
                targetAttempt = soleAttempt;
            }
        }

        // Si no se pudo identificar inequívocamente el intento con evidencia externa, NO asociar a ciegas ni acreditar
        if (targetAttempt == null) {
            log.error("Imposible asociar la transacción financiera {} a ningún intento inequívoco para el pago {}",
                    queryResult.providerPaymentId(), paymentId);
            throw new ConflictException("PAYMENT_ATTEMPT_MISMATCH",
                    "No se encontró un intento de pago inequívoco que corresponda a la transacción informada.");
        }

        String mpStatus = queryResult.status().toLowerCase(Locale.ROOT);
        String mpStatusDetail = queryResult.statusDetail() != null ? queryResult.statusDetail().toLowerCase(Locale.ROOT) : "";
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

        // 3. Transición según estado financiero y acreditación real de la transacción:
        // Se considera acreditado financieramente si:
        // a) El pago individual tiene status="approved", o
        // b) El pago tiene status="processed" CON status_detail="accredited" estricto.
        boolean isAccredited = "approved".equals(mpStatus)
                || ("processed".equals(mpStatus) && "accredited".equals(mpStatusDetail));

        if (isAccredited) {
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
            log.info("Pago {} y su intento {} actualizados a APPROVED conforme a Mercado Pago (status={}, detail={}).",
                    paymentId, targetAttempt != null ? targetAttempt.getId() : "N/A", mpStatus, mpStatusDetail);
        } else if ("rejected".equals(mpStatus) || "cancelled".equals(mpStatus)) {
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
        } else if ("pending".equals(mpStatus) || "in_process".equals(mpStatus) || "authorized".equals(mpStatus) || "processed".equals(mpStatus)) {
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
            log.info("Pago {} e intento en estado intermedio: {} (detail={}).", paymentId, mpStatus, mpStatusDetail);
        } else {
            log.warn("Estado desconocido de Mercado Pago ({}, detail={}) recibido para pago {}.", mpStatus, mpStatusDetail, paymentId);
        }

        String attemptStatus = targetAttempt != null ? targetAttempt.getStatus().name() : "NONE";
        return ProcessMercadoPagoWebhookResult.success(
                paymentId, queryResult.providerPaymentId(), payment.getStatus().name(), attemptStatus,
                "Estado del pago actualizado conforme a consulta con Mercado Pago."
        );
    }

    /**
     * Aplica la cancelación o expiración de una orden de Orders API cuando no contiene transacciones financieras.
     * Marca el PaymentAttempt correspondiente como REJECTED preservando el Payment principal en PENDING para reintento.
     */
    @Transactional
    public ProcessMercadoPagoWebhookResult applyOrderCancellation(
            UUID paymentId,
            String providerOrderId,
            String orderStatus,
            String orderStatusDetail
    ) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado para ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("Pago {} ya se encuentra APPROVED. Ignorando cancelación tardía de orden {}.", paymentId, providerOrderId);
            return ProcessMercadoPagoWebhookResult.success(
                    paymentId, null, payment.getStatus().name(), "APPROVED",
                    "Orden cancelada extemporáneamente; estado APPROVED preservado por idempotencia."
            );
        }

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId);
        PaymentAttempt targetAttempt = null;

        if (providerOrderId != null && !providerOrderId.isBlank()) {
            for (PaymentAttempt a : attempts) {
                if (providerOrderId.equals(a.getProviderOrderId())) {
                    targetAttempt = a;
                    break;
                }
            }
        }
        if (targetAttempt == null && !attempts.isEmpty()) {
            PaymentAttempt last = attempts.getLast();
            if (last.getStatus() == PaymentAttemptStatus.PROCESSING || last.getStatus() == PaymentAttemptStatus.CREATED) {
                targetAttempt = last;
            }
        }

        if (targetAttempt != null) {
            targetAttempt.markRejected(
                    null,
                    orderStatus,
                    orderStatusDetail,
                    "ORDER_CANCELED",
                    "Orden cancelada o expirada en Mercado Pago: " + (orderStatusDetail != null ? orderStatusDetail : orderStatus),
                    Instant.now()
            );
            paymentAttemptRepository.save(targetAttempt);
            log.info("Intento {} marcado como REJECTED por cancelación de orden {} en Mercado Pago.",
                    targetAttempt.getId(), providerOrderId);
        }

        String attemptStatus = targetAttempt != null ? targetAttempt.getStatus().name() : "NONE";
        return ProcessMercadoPagoWebhookResult.success(
                paymentId, null, payment.getStatus().name(), attemptStatus,
                "Orden cancelada o expirada registrada en el intento de pago."
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
