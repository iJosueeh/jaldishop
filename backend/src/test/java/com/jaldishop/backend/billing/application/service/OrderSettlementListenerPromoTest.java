package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.application.port.out.FeePolicyProvider;
import com.jaldishop.backend.billing.domain.model.FeePolicy;
import com.jaldishop.backend.billing.domain.model.LedgerEntryType;
import com.jaldishop.backend.billing.domain.model.PaymentBreakdown;
import com.jaldishop.backend.billing.domain.service.FeeCalculatorDomainService;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.shared.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSettlementListenerPromoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FeePolicyProvider feePolicyProvider;

    private final FeeCalculatorDomainService feeCalculator = new FeeCalculatorDomainService();

    @Test
    void merchantFundedPromoReducesLedgerSaleCreditAndCommissionBase() {
        var listener = new OrderSettlementListener(jdbcTemplate, feePolicyProvider, feeCalculator);
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(feePolicyProvider.getEffectivePolicy()).thenReturn(FeePolicy.defaultPeruCommerce());
        when(jdbcTemplate.queryForList(anyString(), eq(orderId))).thenReturn(List.of(Map.of(
                "id", orderId,
                "store_id", storeId,
                "user_id", userId,
                "delivery_mode", "PICKUP",
                "payment_method", "ONLINE",
                "products_subtotal_amount", new BigDecimal("100.00"),
                "discount_amount", new BigDecimal("20.00"),
                "promo_funding_source", "MERCHANT",
                "delivery_fee_amount", BigDecimal.ZERO,
                "currency", "PEN"
        )));
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(storeId))).thenReturn(BigDecimal.ZERO);

        listener.onOrderStatusChanged(new OrderStatusChangedDomainEvent(
                orderId, "ORD-1", userId, storeId, OrderStatus.CONFIRMED, OrderStatus.COMPLETED
        ));

        ArgumentCaptor<BigDecimal> creditCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(jdbcTemplate).update(anyString(), eq(storeId), eq(orderId), eq(LedgerEntryType.SALE_CREDIT.name()),
                eq(BigDecimal.ZERO), creditCaptor.capture(), any(), eq("PEN"), anyString(), eq("SALE_" + orderId));

        assertThat(creditCaptor.getValue()).isEqualByComparingTo("80.00");
    }

    @Test
    void platformFundedPromoPreservesFullMerchantSubtotalInLedger() {
        var listener = new OrderSettlementListener(jdbcTemplate, feePolicyProvider, feeCalculator);
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(feePolicyProvider.getEffectivePolicy()).thenReturn(FeePolicy.defaultPeruCommerce());
        when(jdbcTemplate.queryForList(anyString(), eq(orderId))).thenReturn(List.of(Map.of(
                "id", orderId,
                "store_id", storeId,
                "user_id", userId,
                "delivery_mode", "PICKUP",
                "payment_method", "ONLINE",
                "products_subtotal_amount", new BigDecimal("100.00"),
                "discount_amount", new BigDecimal("20.00"),
                "promo_funding_source", "PLATFORM",
                "delivery_fee_amount", BigDecimal.ZERO,
                "currency", "PEN"
        )));
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(storeId))).thenReturn(BigDecimal.ZERO);

        listener.onOrderStatusChanged(new OrderStatusChangedDomainEvent(
                orderId, "ORD-2", userId, storeId, OrderStatus.CONFIRMED, OrderStatus.COMPLETED
        ));

        ArgumentCaptor<BigDecimal> creditCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(jdbcTemplate).update(anyString(), eq(storeId), eq(orderId), eq(LedgerEntryType.SALE_CREDIT.name()),
                eq(BigDecimal.ZERO), creditCaptor.capture(), any(), eq("PEN"), anyString(), eq("SALE_" + orderId));

        assertThat(creditCaptor.getValue()).isEqualByComparingTo("100.00");
    }
}
