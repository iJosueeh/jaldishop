package com.jaldishop.backend.payment.application.gateway;

import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoResourceValidatorTest {
    @Test void blankTransactionIdentityIsRejected() {
        var validator = new MercadoPagoResourceValidator(new MercadoPagoProperties());
        var item = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult.OrderPaymentItem(
                "", "approved", "accredited", java.math.BigDecimal.TEN, "PEN", null, null, null);
        var order = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORD", "processed", "accredited", java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                java.util.UUID.randomUUID().toString(), java.util.List.of(item), null, false);
        assertThrows(com.jaldishop.backend.shared.exception.ConflictException.class, () -> validator.validateOrder("ORD", order));
    }

    @Test void expectedAccountAndEnvironmentMustBePresentAndMatch() {
        var properties = new MercadoPagoProperties();
        properties.setExpectedCollectorId(123L);
        properties.setExpectedLiveMode(false);
        var validator = new MercadoPagoResourceValidator(properties);
        assertDoesNotThrow(() -> validator.validateAccount(123L, false));
        assertThrows(SecurityException.class, () -> validator.validateAccount(null, false));
        assertThrows(SecurityException.class, () -> validator.validateAccount(456L, false));
        assertThrows(SecurityException.class, () -> validator.validateAccount(123L, null));
        assertThrows(SecurityException.class, () -> validator.validateAccount(123L, true));
    }
}
