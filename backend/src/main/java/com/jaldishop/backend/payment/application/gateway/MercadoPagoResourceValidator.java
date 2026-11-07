package com.jaldishop.backend.payment.application.gateway;

import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

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
        if (properties.getExpectedLiveMode() != null && !properties.getExpectedLiveMode().equals(liveMode)) {
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
        validateAccount(order.collectorId(), order.liveMode());
    }
}
