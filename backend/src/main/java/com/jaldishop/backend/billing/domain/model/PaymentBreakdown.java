package com.jaldishop.backend.billing.domain.model;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.shared.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object inmutable que representa el desglose financiero exacto de una orden.
 * Diseñado conforme a Domain-Driven Design y principios Fintech:
 * - Aritmética de precisión con Banker's Rounding (HALF_EVEN)
 * - Protección del margen de delivery (comisión aplica 0% sobre delivery)
 * - Desacoplamiento total de la pasarela de pagos
 */
public record PaymentBreakdown(
        UUID orderId,
        UUID storeId,
        UUID userId,
        DeliveryMode deliveryMode,
        String paymentMethod,
        Money subtotalAmount,
        Money deliveryFee,
        Money platformFee,
        Money platformFeeBase,
        Money platformFeeIgv,
        Money merchantCommissionBase,
        Money merchantCommissionIgv,
        Money merchantCommissionTotal,
        Money merchantNetAmount,
        Money totalPaidByCustomer,
        Money gatewayFeeEstimated
) {
    public PaymentBreakdown {
        Objects.requireNonNull(orderId, "OrderId no puede ser nulo");
        Objects.requireNonNull(storeId, "StoreId no puede ser nulo");
        Objects.requireNonNull(userId, "UserId no puede ser nulo");
        Objects.requireNonNull(deliveryMode, "DeliveryMode no puede ser nulo");
        Objects.requireNonNull(paymentMethod, "PaymentMethod no puede ser nulo");
        Objects.requireNonNull(subtotalAmount, "SubtotalAmount no puede ser nulo");
        Objects.requireNonNull(deliveryFee, "DeliveryFee no puede ser nulo");
        Objects.requireNonNull(platformFee, "PlatformFee no puede ser nulo");
        Objects.requireNonNull(totalPaidByCustomer, "TotalPaidByCustomer no puede ser nulo");
        Objects.requireNonNull(merchantNetAmount, "MerchantNetAmount no puede ser nulo");

        // Invariante de entrega en tienda: Recojo en tienda no puede devengar costo de delivery
        if (deliveryMode == DeliveryMode.PICKUP && !deliveryFee.isZero()) {
            throw new IllegalArgumentException("En modalidad PICKUP la tarifa de delivery debe ser estrictamente S/ 0.00");
        }
    }

    public static PaymentBreakdown calculate(
            UUID orderId,
            UUID storeId,
            UUID userId,
            DeliveryMode deliveryMode,
            String paymentMethod,
            Money subtotal,
            Money rawDeliveryFee,
            FeePolicy policy
    ) {
        Objects.requireNonNull(policy, "FeePolicy no puede ser nula");
        String currency = subtotal.currency();

        // 1. En modalidad PICKUP el delivery es estrictamente 0
        Money effectiveDelivery = (deliveryMode == DeliveryMode.PICKUP) ? Money.zero(currency) : rawDeliveryFee;
        BigDecimal igvRate = policy.igvRate();

        // 2. Tarifa de servicio al comprador (Platform Fee)
        // Se bonifica si el subtotal de productos supera o iguala el umbral de exoneración (ej. S/ 60.00)
        Money customerFee = Money.zero(currency);
        if (policy.isActive() && subtotal.amount().compareTo(policy.customerServiceFeeThresholdWaive()) < 0) {
            customerFee = Money.of(policy.customerServiceFeeFixed(), currency);
        }

        BigDecimal onePlusIgv = BigDecimal.ONE.add(igvRate);
        BigDecimal platformBaseAmt = customerFee.amount().divide(onePlusIgv, Money.SCALE, RoundingMode.HALF_EVEN);
        Money platformFeeBase = Money.of(platformBaseAmt, currency);
        Money platformFeeIgv = customerFee.subtract(platformFeeBase);

        // 3. Comisión mercantil al comercio (Take Rate de JaldiShop)
        // REGLA CLAVE: La comisión se aplica EXCLUSIVAMENTE sobre el subtotal de alimentos/productos.
        // NUNCA grava la tarifa de delivery del comerciante.
        boolean isOnlinePayment = "ONLINE".equalsIgnoreCase(paymentMethod) || "CARD".equalsIgnoreCase(paymentMethod);
        BigDecimal takeRatePct = isOnlinePayment ? policy.merchantTakeRatePct() : policy.merchantCashTakeRatePct();

        BigDecimal commBaseAmt = subtotal.amount().multiply(takeRatePct).setScale(Money.SCALE, RoundingMode.HALF_EVEN);
        Money merchantCommissionBase = Money.of(commBaseAmt, currency);

        BigDecimal commIgvAmt = commBaseAmt.multiply(igvRate).setScale(Money.SCALE, RoundingMode.HALF_EVEN);
        Money merchantCommissionIgv = Money.of(commIgvAmt, currency);

        Money merchantCommissionTotal = merchantCommissionBase.add(merchantCommissionIgv);

        // 4. Totales finales
        // El cliente paga: Subtotal productos + Delivery + Tarifa de servicio
        Money totalPaid = subtotal.add(effectiveDelivery).add(customerFee);

        // La tienda recibe: Subtotal productos - Comisión JaldiShop + 100% de su tarifa de delivery
        Money merchantNet = subtotal.subtract(merchantCommissionTotal).add(effectiveDelivery);

        // 5. Estimación de costo de pasarela (3.45% + S/ 0.30 + IGV) si es online
        Money estGatewayFee = Money.zero(currency);
        if (isOnlinePayment) {
            BigDecimal gatewayPct = new BigDecimal("0.0345");
            BigDecimal gatewayFixed = new BigDecimal("0.30");
            BigDecimal gatewayBase = totalPaid.amount().multiply(gatewayPct).add(gatewayFixed);
            BigDecimal gatewayWithTax = gatewayBase.multiply(onePlusIgv).setScale(Money.SCALE, RoundingMode.HALF_EVEN);
            estGatewayFee = Money.of(gatewayWithTax, currency);
        }

        return new PaymentBreakdown(
                orderId,
                storeId,
                userId,
                deliveryMode,
                paymentMethod,
                subtotal,
                effectiveDelivery,
                customerFee,
                platformFeeBase,
                platformFeeIgv,
                merchantCommissionBase,
                merchantCommissionIgv,
                merchantCommissionTotal,
                merchantNet,
                totalPaid,
                estGatewayFee
        );
    }
}
