package com.jaldishop.backend.billing.domain.service;

import com.jaldishop.backend.billing.domain.model.FeePolicy;
import com.jaldishop.backend.billing.domain.model.PaymentBreakdown;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.shared.domain.model.Money;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio de Dominio para el cálculo y auditoría de tarifas de JaldiShop.
 * Desacoplado de la infraestructura HTTP y base de datos.
 */
@Service
public class FeeCalculatorDomainService {

    public PaymentBreakdown calculateBreakdown(
            UUID orderId,
            UUID storeId,
            UUID userId,
            DeliveryMode deliveryMode,
            String paymentMethod,
            Money productsSubtotal,
            Money deliveryFee,
            FeePolicy policy
    ) {
        FeePolicy effectivePolicy = (policy != null) ? policy : FeePolicy.defaultPeruCommerce();
        return PaymentBreakdown.calculate(
                orderId,
                storeId,
                userId,
                deliveryMode,
                paymentMethod,
                productsSubtotal,
                deliveryFee,
                effectivePolicy
        );
    }
}
