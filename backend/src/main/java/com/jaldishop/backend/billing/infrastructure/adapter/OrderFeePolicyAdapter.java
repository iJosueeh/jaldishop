package com.jaldishop.backend.billing.infrastructure.adapter;

import com.jaldishop.backend.billing.application.port.out.FeePolicyProvider;
import com.jaldishop.backend.billing.domain.model.FeePolicy;
import com.jaldishop.backend.ordering.application.port.out.OrderFeePolicyPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderFeePolicyAdapter implements OrderFeePolicyPort {

    private final FeePolicyProvider feePolicyProvider;

    public OrderFeePolicyAdapter(FeePolicyProvider feePolicyProvider) {
        this.feePolicyProvider = feePolicyProvider;
    }

    @Override
    public BigDecimal getTakeRate() {
        FeePolicy policy = feePolicyProvider.getEffectivePolicy();
        return policy != null ? policy.merchantTakeRatePct() : new BigDecimal("0.0350");
    }

    @Override
    public BigDecimal getFixedPlatformFee() {
        FeePolicy policy = feePolicyProvider.getEffectivePolicy();
        return policy != null ? policy.customerServiceFeeFixed() : new BigDecimal("0.90");
    }
}
