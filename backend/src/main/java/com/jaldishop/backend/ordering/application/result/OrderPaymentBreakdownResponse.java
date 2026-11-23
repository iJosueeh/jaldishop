package com.jaldishop.backend.ordering.application.result;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record OrderPaymentBreakdownResponse(
        BigDecimal subtotalAmount,
        BigDecimal deliveryFee,
        BigDecimal platformFee,
        BigDecimal merchantCommissionAmount,
        BigDecimal merchantNetAmount,
        BigDecimal totalPaidByCustomer
) {
    public static OrderPaymentBreakdownResponse compute(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal takeRatePct, BigDecimal platformFeeFixed) {
        BigDecimal sub = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal del = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        BigDecimal rate = takeRatePct != null ? takeRatePct : new BigDecimal("0.0350"); // 3.5%
        BigDecimal plat = platformFeeFixed != null ? platformFeeFixed : new BigDecimal("0.90");

        BigDecimal comm = sub.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal net = sub.add(del).subtract(comm).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalPaid = sub.add(del).add(plat).setScale(2, RoundingMode.HALF_EVEN);

        return new OrderPaymentBreakdownResponse(sub, del, plat, comm, net, totalPaid);
    }
}
