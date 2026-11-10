package com.jaldishop.backend.payment.application.gateway;

import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
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

    @Test void validateOrderAcceptsRealSandboxOrderWithoutLiveModeWhenCollectorAndIdMatch() {
        var properties = new MercadoPagoProperties();
        properties.setExpectedCollectorId(3819941792L);
        properties.setExpectedLiveMode(false);
        var validator = new MercadoPagoResourceValidator(properties);

        var order = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORDTST01M71YY0FMNBE787P2QH8JBM60", "created", null, java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                "pa1_" + java.util.UUID.randomUUID(), java.util.List.of(), List.of(), 3819941792L, null, "https://mp.com"
        );
        assertDoesNotThrow(() -> validator.validateOrder("ORDTST01M71YY0FMNBE787P2QH8JBM60", order));
    }

    @Test void validateOrderRejectsOrderWithExplicitLiveModeTrue() {
        var properties = new MercadoPagoProperties();
        properties.setExpectedCollectorId(3819941792L);
        properties.setExpectedLiveMode(false);
        var validator = new MercadoPagoResourceValidator(properties);

        var order = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORDTST01M71YY0FMNBE787P2QH8JBM60", "created", null, java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                "pa1_" + java.util.UUID.randomUUID(), java.util.List.of(), List.of(), 3819941792L, true, "https://mp.com"
        );
        assertThrows(SecurityException.class, () -> validator.validateOrder("ORDTST01M71YY0FMNBE787P2QH8JBM60", order));
    }

    @Test void validateOrderRejectsIncompatibleOrderIdForTest() {
        var properties = new MercadoPagoProperties();
        properties.setExpectedCollectorId(3819941792L);
        properties.setExpectedLiveMode(false);
        var validator = new MercadoPagoResourceValidator(properties);

        // Prefijo de producción ORD0... sin TST
        var order = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORD01PRODUCTION999", "created", null, java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                "pa1_" + java.util.UUID.randomUUID(), java.util.List.of(), List.of(), 3819941792L, null, "https://mp.com"
        );
        assertThrows(SecurityException.class, () -> validator.validateOrder("ORD01PRODUCTION999", order));
    }

    @Test void validateOrderRejectsMismatchedOrMissingCollectorId() {
        var properties = new MercadoPagoProperties();
        properties.setExpectedCollectorId(3819941792L);
        properties.setExpectedLiveMode(false);
        var validator = new MercadoPagoResourceValidator(properties);

        var orderMismatched = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORDTST01M71YY0FMNBE787P2QH8JBM60", "created", null, java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                "pa1_" + java.util.UUID.randomUUID(), java.util.List.of(), List.of(), 9999999L, null, "https://mp.com"
        );
        assertThrows(SecurityException.class, () -> validator.validateOrder("ORDTST01M71YY0FMNBE787P2QH8JBM60", orderMismatched));

        var orderMissing = new com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult(
                "ORDTST01M71YY0FMNBE787P2QH8JBM60", "created", null, java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO,
                "pa1_" + java.util.UUID.randomUUID(), java.util.List.of(), List.of(), null, null, "https://mp.com"
        );
        assertThrows(SecurityException.class, () -> validator.validateOrder("ORDTST01M71YY0FMNBE787P2QH8JBM60", orderMissing));
    }

    @Test void isCompatibleTestOrderIdStrictlyRequiresOrdtstPrefix() {
        assertTrue(MercadoPagoResourceValidator.isCompatibleTestOrderId("ORDTST01M71YY0FMNBE787P2QH8JBM60"));
        assertTrue(MercadoPagoResourceValidator.isCompatibleTestOrderId("ordtst01m71yy0fmnbe787p2qh8jbm60"));
        assertTrue(MercadoPagoResourceValidator.isCompatibleTestOrderId("  ORDTST123  "));

        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("ORD-123"));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("TEST-123"));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("STUB-123"));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("ORD01PRODUCTION999"));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("PROD-123"));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId(""));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId("   "));
        assertFalse(MercadoPagoResourceValidator.isCompatibleTestOrderId(null));
    }

    @Test void resolvedCollectorIdChecksContradictionsAndMatchesStrictly() {
        // Both present and match
        var matchingBoth = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                3819941792L, null, null, null, "3819941792"
        );
        assertEquals(3819941792L, matchingBoth.resolvedCollectorId());

        // Both present and contradictory -> IllegalStateException
        var contradictory = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                12345L, null, null, null, "3819941792"
        );
        assertThrows(IllegalStateException.class, contradictory::resolvedCollectorId);

        // Only userId present
        var onlyUser = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                null, null, null, null, "3819941792"
        );
        assertEquals(3819941792L, onlyUser.resolvedCollectorId());

        // Only collectorId present
        var onlyCollector = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                3819941792L, null, null, null, null
        );
        assertEquals(3819941792L, onlyCollector.resolvedCollectorId());

        // Invalid non-numeric userId -> IllegalStateException
        var invalidUser = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                null, null, null, null, "not-a-number"
        );
        assertThrows(IllegalStateException.class, invalidUser::resolvedCollectorId);

        // Neither present -> null
        var none = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto.MpOrderResponse(
                "ORDTST1", "online", "created", null, "manual", null, null, null, null, null, null,
                null, null, null, null, null
        );
        assertNull(none.resolvedCollectorId());
    }
}
