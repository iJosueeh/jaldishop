package com.jaldishop.backend.payment.application.gateway;

import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Trust checks shared by webhook reads and manual/scheduled reconciliation. */
@Component
public class MercadoPagoResourceValidator {
    private final MercadoPagoProperties properties;

    public MercadoPagoResourceValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    public void validateAccount(Long collectorId, Boolean liveMode) {
        if (properties.getExpectedCollectorId() != null && properties.getExpectedCollectorId() > 0
                && !properties.getExpectedCollectorId().equals(collectorId)) {
            throw new SecurityException("Cuenta receptora ausente o no autorizada (collector_id=" + collectorId + ").");
        }
        if (properties.getExpectedLiveMode() != null && !Boolean.FALSE.equals(liveMode)) {
            throw new SecurityException("Entorno remoto ausente o no autorizado (live_mode=" + liveMode + ").");
        }
    }

    public void validateOrder(String expectedOrderId, OrderQueryResult order) {
        if (order == null || expectedOrderId == null || !expectedOrderId.equals(order.orderId())) {
            throw new ConflictException("ORDER_ID_MISMATCH", "Respuesta de orden ausente o contradictoria.");
        }
        if (order.payments() == null || order.payments().size() > 1) {
            throw new ConflictException("AMBIGUOUS_ORDER_PAYMENTS", "Transacciones de orden inválidas o ambiguas.");
        }
        if (!order.payments().isEmpty() && (order.payments().getFirst() == null
                || order.payments().getFirst().providerPaymentId() == null
                || order.payments().getFirst().providerPaymentId().isBlank())) {
            throw new ConflictException("MISSING_REMOTE_PAYMENT_ID", "La transacción no tiene identidad remota.");
        }
        validateOrderEnvironment(order);
    }

    /**
     * Valida el entorno y la titularidad de una orden remota.
     * En Orders API, Mercado Pago no incluye live_mode en el recurso Order (es null).
     * La verificación TEST fail-closed se garantiza mediante:
     * 1. Titularidad del collector_id / user_id esperado (cuenta del vendedor).
     * 2. Prefijo del ID de orden compatible con Sandbox (ORDTST oficial o prefijos de test).
     * 3. Rechazo estricto si live_mode llega explícitamente como true.
     */
    public void validateOrderEnvironment(OrderQueryResult order) {
        if (properties.getExpectedCollectorId() != null && properties.getExpectedCollectorId() > 0) {
            if (order.collectorId() == null || !properties.getExpectedCollectorId().equals(order.collectorId())) {
                throw new SecurityException("Cuenta receptora ausente o no autorizada en orden (collector_id=" + order.collectorId()
                        + ", esperado=" + properties.getExpectedCollectorId() + ").");
            }
        }
        if (Boolean.FALSE.equals(properties.getExpectedLiveMode())) {
            if (Boolean.TRUE.equals(order.liveMode())) {
                throw new SecurityException("Entorno remoto no autorizado: orden contiene live_mode=true explícito.");
            }
            if (!isCompatibleTestOrderId(order.orderId())) {
                throw new SecurityException("Identificador de orden incompatible con entorno TEST (id=" + order.orderId() + ").");
            }
        }
    }

    public static boolean isCompatibleTestOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        return orderId.trim().toUpperCase(Locale.ROOT).startsWith("ORDTST");
    }
}
