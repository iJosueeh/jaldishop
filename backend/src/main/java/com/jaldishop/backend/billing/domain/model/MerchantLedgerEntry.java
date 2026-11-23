package com.jaldishop.backend.billing.domain.model;

import com.jaldishop.backend.shared.domain.model.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrada inmutable del libro mayor de liquidaciones (Append-Only Ledger).
 * Asegura auditoría y cuadre de caja exacto para cada comercio.
 */
public record MerchantLedgerEntry(
        Long id,
        UUID storeId,
        UUID orderId,
        LedgerEntryType entryType,
        Money debitAmount,
        Money creditAmount,
        Money balanceAfter,
        String referenceNote,
        String idempotencyKey,
        Instant createdAt
) {
    public MerchantLedgerEntry {
        Objects.requireNonNull(storeId, "StoreId no puede ser nulo");
        Objects.requireNonNull(entryType, "EntryType no puede ser nulo");
        Objects.requireNonNull(debitAmount, "DebitAmount no puede ser nulo");
        Objects.requireNonNull(creditAmount, "CreditAmount no puede ser nulo");
        Objects.requireNonNull(balanceAfter, "BalanceAfter no puede ser nulo");
        if (debitAmount.isNegative() || creditAmount.isNegative()) {
            throw new IllegalArgumentException("Montos de débito y crédito deben ser mayores o iguales a cero");
        }
    }

    public static MerchantLedgerEntry createSaleCredit(
            UUID storeId,
            UUID orderId,
            Money currentBalance,
            Money saleAmount,
            String orderNumber
    ) {
        Money newBalance = currentBalance.add(saleAmount);
        return new MerchantLedgerEntry(
                null,
                storeId,
                orderId,
                LedgerEntryType.SALE_CREDIT,
                Money.zero(saleAmount.currency()),
                saleAmount,
                newBalance,
                "Abono por venta de productos pedido #" + orderNumber,
                "SALE_" + orderId,
                Instant.now()
        );
    }

    public static MerchantLedgerEntry createDeliveryCredit(
            UUID storeId,
            UUID orderId,
            Money currentBalance,
            Money deliveryAmount,
            String orderNumber
    ) {
        Money newBalance = currentBalance.add(deliveryAmount);
        return new MerchantLedgerEntry(
                null,
                storeId,
                orderId,
                LedgerEntryType.DELIVERY_CREDIT,
                Money.zero(deliveryAmount.currency()),
                deliveryAmount,
                newBalance,
                "Abono por servicio de delivery propio pedido #" + orderNumber,
                "DELIVERY_" + orderId,
                Instant.now()
        );
    }

    public static MerchantLedgerEntry createCommissionDebit(
            UUID storeId,
            UUID orderId,
            Money currentBalance,
            Money commissionTotal,
            String orderNumber
    ) {
        Money newBalance = currentBalance.subtract(commissionTotal);
        return new MerchantLedgerEntry(
                null,
                storeId,
                orderId,
                LedgerEntryType.COMMISSION_DEBIT,
                commissionTotal,
                Money.zero(commissionTotal.currency()),
                newBalance,
                "Comisión tecnológica JaldiShop pedido #" + orderNumber,
                "COMMISSION_" + orderId,
                Instant.now()
        );
    }
}
