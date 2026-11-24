package com.jaldishop.backend.billing.application.port.out;

import com.jaldishop.backend.billing.domain.model.FeePolicy;

public interface FeePolicyProvider {
    FeePolicy getEffectivePolicy();
}
