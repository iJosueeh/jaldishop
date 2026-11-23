package com.jaldishop.backend.billing.domain;

import com.jaldishop.backend.billing.domain.model.FeePolicy;
import com.jaldishop.backend.billing.domain.model.MerchantLedgerEntry;
import com.jaldishop.backend.billing.domain.model.PaymentBreakdown;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentBreakdownTest {

    private final FeePolicy defaultPolicy = FeePolicy.defaultPeruCommerce();
    private final UUID orderId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Escenario A: Ticket bajo S/ 18.00 con Recojo en Tienda (PICKUP)")
    void testPickupModeForcesDeliveryFeeToZeroAndCalculatesExactFees() {
        PaymentBreakdown breakdown = PaymentBreakdown.calculate(
                orderId,
                storeId,
                userId,
                DeliveryMode.PICKUP,
                "ONLINE",
                Money.pen(18.00),
                Money.pen(5.00), // Aunque se mande delivery, pickup debe forzar S/ 0.00
                defaultPolicy
        );

        // Verificaciones de entrega
        assertThat(breakdown.deliveryMode()).isEqualTo(DeliveryMode.PICKUP);
        assertThat(breakdown.deliveryFee()).isEqualTo(Money.pen(0.00));

        // Tarifa al comprador: S/ 0.90 porque subtotal (18.00) < 60.00
        assertThat(breakdown.platformFee()).isEqualTo(Money.pen(0.90));
        // Base IGV plataforma: 0.90 / 1.18 = 0.76 base, 0.14 IGV
        assertThat(breakdown.platformFeeBase()).isEqualTo(Money.pen(0.76));
        assertThat(breakdown.platformFeeIgv()).isEqualTo(Money.pen(0.14));

        // Comisión al comercio: 3.5% sobre S/ 18.00 = S/ 0.63 base. IGV 18% = S/ 0.11. Total comision = S/ 0.74
        assertThat(breakdown.merchantCommissionBase()).isEqualTo(Money.pen(0.63));
        assertThat(breakdown.merchantCommissionIgv()).isEqualTo(Money.pen(0.11));
        assertThat(breakdown.merchantCommissionTotal()).isEqualTo(Money.pen(0.74));

        // Total pagado por cliente: 18.00 + 0.00 + 0.90 = S/ 18.90
        assertThat(breakdown.totalPaidByCustomer()).isEqualTo(Money.pen(18.90));

        // Liquidación neta comercio: 18.00 - 0.74 = S/ 17.26
        assertThat(breakdown.merchantNetAmount()).isEqualTo(Money.pen(17.26));

        // Invariante matemática
        assertThat(breakdown.totalPaidByCustomer())
                .isEqualTo(breakdown.subtotalAmount().add(breakdown.deliveryFee()).add(breakdown.platformFee()));
        assertThat(breakdown.merchantNetAmount())
                .isEqualTo(breakdown.subtotalAmount().add(breakdown.deliveryFee()).subtract(breakdown.merchantCommissionTotal()));
    }

    @Test
    @DisplayName("Escenario B: Ticket promedio S/ 45.00 con Delivery Propio (100% de delivery para la tienda)")
    void testMerchantDeliveryFeeIsNotTaxedByCommission() {
        PaymentBreakdown breakdown = PaymentBreakdown.calculate(
                orderId,
                storeId,
                userId,
                DeliveryMode.DELIVERY,
                "ONLINE",
                Money.pen(45.00),
                Money.pen(5.00),
                defaultPolicy
        );

        assertThat(breakdown.deliveryFee()).isEqualTo(Money.pen(5.00));
        assertThat(breakdown.platformFee()).isEqualTo(Money.pen(0.90));

        // Comisión de JaldiShop: 3.5% de S/ 45.00 = S/ 1.58 base. IGV 18% = S/ 0.28. Total comisión = S/ 1.86
        // REGLA CLAVE: S/ 5.00 de delivery no sufre deducción
        assertThat(breakdown.merchantCommissionBase()).isEqualTo(Money.pen(1.58));
        assertThat(breakdown.merchantCommissionIgv()).isEqualTo(Money.pen(0.28));
        assertThat(breakdown.merchantCommissionTotal()).isEqualTo(Money.pen(1.86));

        // Total cliente: 45.00 + 5.00 + 0.90 = S/ 50.90
        assertThat(breakdown.totalPaidByCustomer()).isEqualTo(Money.pen(50.90));

        // Tienda recibe: 45.00 (platos) - 1.86 (comisión) + 5.00 (delivery íntegro) = S/ 48.14
        assertThat(breakdown.merchantNetAmount()).isEqualTo(Money.pen(48.14));
    }

    @Test
    @DisplayName("Escenario C: Ticket alto S/ 120.00 con Tarifa de Servicio Bonificada (S/ 0.00)")
    void testServiceFeeIsWaivedForHighTickets() {
        PaymentBreakdown breakdown = PaymentBreakdown.calculate(
                orderId,
                storeId,
                userId,
                DeliveryMode.DELIVERY,
                "ONLINE",
                Money.pen(120.00),
                Money.pen(7.00),
                defaultPolicy
        );

        // Tarifa bonificada por superar umbral S/ 60.00
        assertThat(breakdown.platformFee()).isEqualTo(Money.pen(0.00));
        assertThat(breakdown.platformFeeBase()).isEqualTo(Money.pen(0.00));
        assertThat(breakdown.platformFeeIgv()).isEqualTo(Money.pen(0.00));

        // Comisión: 3.5% de S/ 120.00 = S/ 4.20 base. IGV 18% = S/ 0.76. Total = S/ 4.96
        assertThat(breakdown.merchantCommissionTotal()).isEqualTo(Money.pen(4.96));

        // Total cliente: 120.00 + 7.00 + 0.00 = S/ 127.00
        assertThat(breakdown.totalPaidByCustomer()).isEqualTo(Money.pen(127.00));

        // Tienda recibe: 120.00 - 4.96 + 7.00 = S/ 122.04
        assertThat(breakdown.merchantNetAmount()).isEqualTo(Money.pen(122.04));
    }

    @Test
    @DisplayName("Escenario D: Pago directo en efectivo o Yape offline (Tasa reducida 2.0%)")
    void testDirectCashOrYapeReducesTakeRate() {
        PaymentBreakdown breakdown = PaymentBreakdown.calculate(
                orderId,
                storeId,
                userId,
                DeliveryMode.DELIVERY,
                "CASH_ON_DELIVERY",
                Money.pen(50.00),
                Money.pen(4.00),
                defaultPolicy
        );

        // Tasa 2.0% sobre S/ 50.00 = S/ 1.00 base. IGV 18% = S/ 0.18. Total = S/ 1.18
        assertThat(breakdown.merchantCommissionBase()).isEqualTo(Money.pen(1.00));
        assertThat(breakdown.merchantCommissionIgv()).isEqualTo(Money.pen(0.18));
        assertThat(breakdown.merchantCommissionTotal()).isEqualTo(Money.pen(1.18));

        // Costo de pasarela es 0 en pago directo
        assertThat(breakdown.gatewayFeeEstimated()).isEqualTo(Money.pen(0.00));
    }

    @Test
    @DisplayName("Libro Mayor: Transacciones de partida doble para conciliación")
    void testMerchantLedgerEntries() {
        Money initialBalance = Money.pen(100.00);

        // Abono de venta
        MerchantLedgerEntry saleEntry = MerchantLedgerEntry.createSaleCredit(
                storeId, orderId, initialBalance, Money.pen(45.00), "ORD-001"
        );
        assertThat(saleEntry.balanceAfter()).isEqualTo(Money.pen(145.00));

        // Abono de delivery propio
        MerchantLedgerEntry deliveryEntry = MerchantLedgerEntry.createDeliveryCredit(
                storeId, orderId, saleEntry.balanceAfter(), Money.pen(5.00), "ORD-001"
        );
        assertThat(deliveryEntry.balanceAfter()).isEqualTo(Money.pen(150.00));

        // Débito de comisión
        MerchantLedgerEntry commEntry = MerchantLedgerEntry.createCommissionDebit(
                storeId, orderId, deliveryEntry.balanceAfter(), Money.pen(1.86), "ORD-001"
        );
        assertThat(commEntry.balanceAfter()).isEqualTo(Money.pen(148.14));
    }
}
