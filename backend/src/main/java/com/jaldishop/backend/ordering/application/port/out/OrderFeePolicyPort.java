package com.jaldishop.backend.ordering.application.port.out;

import java.math.BigDecimal;

public interface OrderFeePolicyPort {
    BigDecimal getTakeRate();
    BigDecimal getFixedPlatformFee();
}
