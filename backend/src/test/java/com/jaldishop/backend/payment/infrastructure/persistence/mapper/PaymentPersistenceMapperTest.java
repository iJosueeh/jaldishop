package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentAttemptEntity;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceMapperTest {

    private final PaymentPersistenceMapper paymentMapper = new PaymentPersistenceMapper();
    private final PaymentAttemptPersistenceMapper attemptMapper = new PaymentAttemptPersistenceMapper();

    @Test
    @DisplayName("PaymentPersistenceMapper: mapeo bidireccional preserva todos los campos de pagos y snapshots con FulfillmentType")
    void shouldMapPaymentDomainAndEntityBidirectionally() {
        Payment payment = Payment.createPending(
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                "PEN",
                FulfillmentType.DELIVERY,
                "Mario Casas",
                "+51987112233",
                "mario@example.com",
                "Calle Los Pinos 789",
                "Piso 2",
                new BigDecimal("-12.112233"),
                new BigDecimal("-77.033445"),
                new BigDecimal("140.00"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("10.00"),
                new BigDecimal("21.35"),
                "[{\"variantId\":\"f31d0413-5b82-4fcf-b670-3d778d9b1db1\",\"quantity\":3}]"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(new BigDecimal("150.00"));
        payment.completeRefund("REF-12345", Instant.now());

        // Domain -> Entity
        PaymentEntity entity = paymentMapper.toEntity(payment);
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(payment.getId());
        assertThat(entity.getCapacityReservationId()).isEqualTo(payment.getCapacityReservationId());
        assertThat(entity.getAmount()).isEqualByComparingTo("150.00");
        assertThat(entity.getCurrency()).isEqualTo("PEN");
        assertThat(entity.getStatus()).isEqualTo("APPROVED");
        assertThat(entity.getRefundStatus()).isEqualTo("COMPLETED");
        assertThat(entity.getRefundAmount()).isEqualByComparingTo("150.00");
        assertThat(entity.getRefundReference()).isEqualTo("REF-12345");
        assertThat(entity.getFulfillmentType()).isEqualTo("DELIVERY");
        assertThat(entity.getCustomerName()).isEqualTo("Mario Casas");
        assertThat(entity.getDeliveryAddress()).isEqualTo("Calle Los Pinos 789");
        assertThat(entity.getDeliveryLatitude()).isEqualByComparingTo("-12.112233");
        assertThat(entity.getDeliveryLongitude()).isEqualByComparingTo("-77.033445");
        assertThat(entity.getProductsSubtotalAmount()).isEqualByComparingTo("140.00");
        assertThat(entity.getDeliveryFeeAmount()).isEqualByComparingTo("10.00");
        assertThat(entity.getIncludedTaxAmount()).isEqualByComparingTo("21.35");
        assertThat(entity.getItemsSnapshot()).contains("f31d0413-5b82-4fcf-b670-3d778d9b1db1");

        // Entity -> Domain
        Payment rehydrated = paymentMapper.toDomain(entity);
        assertThat(rehydrated).isNotNull();
        assertThat(rehydrated.getId()).isEqualTo(payment.getId());
        assertThat(rehydrated.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(rehydrated.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(rehydrated.getRefundReference()).isEqualTo("REF-12345");
        assertThat(rehydrated.getFulfillmentType()).isEqualTo(FulfillmentType.DELIVERY);
        assertThat(rehydrated.getItemsSnapshot()).isEqualTo(payment.getItemsSnapshot());
    }

    @Test
    @DisplayName("PaymentPersistenceMapper: mapea correctamente pagos históricos con fulfillmentType nulo")
    void shouldMapPaymentEntityWithNullFulfillmentType() {
        PaymentEntity entity = new PaymentEntity(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), "PEN",
                "APPROVED", "NOT_REQUIRED", null, null, Instant.now(), null,
                Instant.now(), Instant.now(), null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );

        Payment domain = paymentMapper.toDomain(entity);
        assertThat(domain).isNotNull();
        assertThat(domain.getFulfillmentType()).isNull();
        assertThat(domain.getCustomerName()).isNull();
    }

    @Test
    @DisplayName("PaymentAttemptPersistenceMapper: mapeo bidireccional preserva campos de intento y preferencia")
    void shouldMapPaymentAttemptDomainAndEntityBidirectionally() {
        PaymentAttempt attempt = PaymentAttempt.create(
                UUID.randomUUID(),
                2,
                PaymentProvider.MERCADO_PAGO,
                PaymentMethod.YAPE,
                "idempotency-key-xyz"
        );
        attempt.recordPreference("pref-xyz", "https://mercadopago.com/checkout/xyz");
        attempt.markApproved("MP-999", "approved", "accredited", PaymentMethod.YAPE, Instant.now());

        // Domain -> Entity
        PaymentAttemptEntity entity = attemptMapper.toEntity(attempt);
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(attempt.getId());
        assertThat(entity.getPaymentId()).isEqualTo(attempt.getPaymentId());
        assertThat(entity.getAttemptNumber()).isEqualTo(2);
        assertThat(entity.getProvider()).isEqualTo("MERCADO_PAGO");
        assertThat(entity.getPaymentMethod()).isEqualTo("YAPE");
        assertThat(entity.getIdempotencyKey()).isEqualTo("idempotency-key-xyz");
        assertThat(entity.getPreferenceId()).isEqualTo("pref-xyz");
        assertThat(entity.getInitPoint()).isEqualTo("https://mercadopago.com/checkout/xyz");
        assertThat(entity.getProviderPaymentId()).isEqualTo("MP-999");
        assertThat(entity.getProviderStatus()).isEqualTo("approved");
        assertThat(entity.getStatus()).isEqualTo("APPROVED");

        // Entity -> Domain
        PaymentAttempt rehydrated = attemptMapper.toDomain(entity);
        assertThat(rehydrated).isNotNull();
        assertThat(rehydrated.getId()).isEqualTo(attempt.getId());
        assertThat(rehydrated.getPaymentMethod()).isEqualTo(PaymentMethod.YAPE);
        assertThat(rehydrated.getPreferenceId()).isEqualTo("pref-xyz");
        assertThat(rehydrated.getProviderPaymentId()).isEqualTo("MP-999");
        assertThat(rehydrated.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
    }

}
