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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderSettlementListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSettlementListener.class);

    private final JdbcTemplate jdbcTemplate;
    private final FeePolicyProvider feePolicyProvider;
    private final FeeCalculatorDomainService feeCalculatorDomainService;

    public OrderSettlementListener(
            JdbcTemplate jdbcTemplate,
            FeePolicyProvider feePolicyProvider,
            FeeCalculatorDomainService feeCalculatorDomainService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.feePolicyProvider = feePolicyProvider;
        this.feeCalculatorDomainService = feeCalculatorDomainService;
    }

    @EventListener
    @Transactional
    public void onOrderStatusChanged(OrderStatusChangedDomainEvent event) {
        if (event.newStatus() != OrderStatus.COMPLETED) {
            return;
        }

        UUID orderId = event.orderId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, store_id, user_id, delivery_mode, payment_method,
                       products_subtotal_amount, discount_amount, promo_funding_source,
                       delivery_fee_amount, currency
                FROM orders
                WHERE id = ?
                """,
                orderId
        );

        if (rows.isEmpty()) {
            log.warn("Orden {} no encontrada al intentar liquidar en el ledger", orderId);
            return;
        }

        Map<String, Object> orderRow = rows.getFirst();
        UUID storeId = UUID.fromString(orderRow.get("store_id").toString());
        UUID userId = UUID.fromString(orderRow.get("user_id").toString());
        DeliveryMode deliveryMode = DeliveryMode.valueOf(orderRow.get("delivery_mode").toString());
        String paymentMethod = orderRow.get("payment_method") != null ? orderRow.get("payment_method").toString() : "ONLINE";
        BigDecimal subtotal = (BigDecimal) orderRow.get("products_subtotal_amount");
        BigDecimal discountAmount = (BigDecimal) orderRow.get("discount_amount");
        String promoFundingSource = orderRow.get("promo_funding_source") != null
                ? orderRow.get("promo_funding_source").toString()
                : null;
        BigDecimal deliveryFee = (BigDecimal) orderRow.get("delivery_fee_amount");
        String currency = orderRow.get("currency") != null ? orderRow.get("currency").toString() : "PEN";

        BigDecimal effectiveSubtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        // Solo los cupones financiados por el comercio (scope = STORE) reducen la base comisionable y el crédito de venta.
        // Los cupones globales (PLATFORM) protegen el ingreso íntegro del comercio.
        if ("MERCHANT".equalsIgnoreCase(promoFundingSource)
                && discountAmount != null
                && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            effectiveSubtotal = effectiveSubtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        }

        FeePolicy policy = feePolicyProvider.getEffectivePolicy();
        PaymentBreakdown breakdown = feeCalculatorDomainService.calculateBreakdown(
                orderId,
                storeId,
                userId,
                deliveryMode,
                paymentMethod,
                new Money(effectiveSubtotal, currency),
                new Money(deliveryFee != null ? deliveryFee : BigDecimal.ZERO, currency),
                policy
        );

        BigDecimal currentBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(credit_amount - debit_amount), 0) FROM merchant_payout_ledgers WHERE store_id = ?",
                BigDecimal.class,
                storeId
        );
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        BigDecimal saleAmount = breakdown.subtotalAmount().amount();
        BigDecimal runningBalance = currentBalance.add(saleAmount);
        insertLedgerEntry(
                storeId,
                orderId,
                LedgerEntryType.SALE_CREDIT,
                BigDecimal.ZERO,
                saleAmount,
                runningBalance,
                currency,
                "Venta completada Orden " + event.orderNumber(),
                "SALE_" + orderId
        );

        BigDecimal delAmount = breakdown.deliveryFee().amount();
        if (delAmount.compareTo(BigDecimal.ZERO) > 0) {
            runningBalance = runningBalance.add(delAmount);
            insertLedgerEntry(
                    storeId,
                    orderId,
                    LedgerEntryType.DELIVERY_CREDIT,
                    BigDecimal.ZERO,
                    delAmount,
                    runningBalance,
                    currency,
                    "Delivery propio Orden " + event.orderNumber(),
                    "DELIVERY_" + orderId
            );
        }

        BigDecimal commissionAmount = breakdown.merchantCommissionTotal().amount();
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            runningBalance = runningBalance.subtract(commissionAmount);
            insertLedgerEntry(
                    storeId,
                    orderId,
                    LedgerEntryType.COMMISSION_DEBIT,
                    commissionAmount,
                    BigDecimal.ZERO,
                    runningBalance,
                    currency,
                    "Comisión plataforma Orden " + event.orderNumber(),
                    "COMM_" + orderId
            );
        }
    }

    private void insertLedgerEntry(
            UUID storeId,
            UUID orderId,
            LedgerEntryType type,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balanceAfter,
            String currency,
            String note,
            String idempotencyKey
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO merchant_payout_ledgers
                (store_id, order_id, entry_type, debit_amount, credit_amount, balance_after, currency, reference_note, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idempotency_key) DO NOTHING
                """,
                storeId,
                orderId,
                type.name(),
                debit,
                credit,
                balanceAfter,
                currency,
                note,
                idempotencyKey
        );
    }
}
