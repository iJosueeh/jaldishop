package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.application.gateway.dto.CreatePreferenceCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderResult;
import com.jaldishop.backend.payment.application.gateway.dto.PreferenceResult;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutRequestRetryTest {
    private static final Instant NOW = Instant.parse("2026-11-05T20:00:00Z");
    private final PaymentPreferencePreparationService preparation = mock(PaymentPreferencePreparationService.class);
    private final MercadoPagoGateway gateway = mock(MercadoPagoGateway.class);
    private final MercadoPagoProperties properties = new MercadoPagoProperties();
    private final Payment payment = mock(Payment.class);
    private final PaymentAttempt attempt = mock(PaymentAttempt.class);
    private final UUID paymentId = UUID.randomUUID();

    private void prepare(String mode) {
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(attempt.getId()).thenReturn(UUID.randomUUID());
        when(attempt.getCheckoutMode()).thenReturn(mode);
        when(attempt.getIdempotencyKey()).thenReturn("same-key");
        var item = new CheckoutItemSnapshot(UUID.randomUUID(), UUID.randomUUID(), "Producto", "Unidad",
                "SKU", null, 1, new BigDecimal("10.00"), "PEN", new BigDecimal("10.00"), false);
        when(preparation.prepareOrGetExistingPayment(any())).thenReturn(
                new PreparationResult(payment, attempt, false, "Tienda", List.of(item), NOW.plusSeconds(600)));
    }

    private CreatePaymentPreferenceService serviceAt(Instant time) {
        return new CreatePaymentPreferenceService(preparation, gateway, properties, new ObjectMapper(),
                Clock.fixed(time, ZoneOffset.UTC));
    }

    private CreatePaymentPreferenceCommand command() {
        return mock(CreatePaymentPreferenceCommand.class);
    }

    @Test
    void preferencesRetryIsEqualWhenInputsAndConfigurationStayTheSame() {
        prepare("PREFERENCES");
        when(gateway.createPreference(any(), any(UUID.class))).thenThrow(new MercadoPagoException(0, "MP_TIMEOUT", "timeout"))
                .thenReturn(new PreferenceResult("pref", "https://example.test", null));
        var request = command();
        assertThatThrownBy(() -> serviceAt(NOW).execute(request)).isInstanceOf(MercadoPagoException.class);
        serviceAt(NOW.plusSeconds(30)).execute(request);

        var captured = ArgumentCaptor.forClass(CreatePreferenceCommand.class);
        verify(gateway, times(2)).createPreference(captured.capture(), any(UUID.class));
        assertThat(captured.getAllValues().get(1)).isEqualTo(captured.getAllValues().get(0));
        assertThat(captured.getValue().externalReference()).isEqualTo(paymentId.toString());
        assertThat(captured.getValue().idempotencyKey()).isEqualTo("same-key");
    }

    @Test
    void preferencesRetryChangesIfConfigurationChanges() {
        prepare("PREFERENCES");
        when(gateway.createPreference(any(), any(UUID.class))).thenThrow(new MercadoPagoException(0, "MP_TIMEOUT", "timeout"))
                .thenReturn(new PreferenceResult("pref", "https://example.test", null));
        var request = command();
        properties.setNotificationUrl("https://example.test/old");
        assertThatThrownBy(() -> serviceAt(NOW).execute(request)).isInstanceOf(MercadoPagoException.class);
        properties.setNotificationUrl("https://example.test/new");
        serviceAt(NOW).execute(request);

        var captured = ArgumentCaptor.forClass(CreatePreferenceCommand.class);
        verify(gateway, times(2)).createPreference(captured.capture(), any(UUID.class));
        assertThat(captured.getAllValues().get(0).idempotencyKey()).isEqualTo(captured.getAllValues().get(1).idempotencyKey());
        assertThat(captured.getAllValues().get(0).notificationUrl()).isNotEqualTo(captured.getAllValues().get(1).notificationUrl());
    }

    @Test
    void ordersRetryChangesExpirationWhileKeepingReferenceAndKey() {
        prepare("ORDERS");
        when(payment.getAmount()).thenReturn(new BigDecimal("10.00"));
        when(gateway.createOrder(any(), any(UUID.class))).thenThrow(new MercadoPagoException(0, "MP_TIMEOUT", "timeout"))
                .thenReturn(new OrderResult("order", "https://example.test", "opened", new BigDecimal("10.00")));
        var request = command();
        assertThatThrownBy(() -> serviceAt(NOW).execute(request)).isInstanceOf(MercadoPagoException.class);
        serviceAt(NOW.plusSeconds(30)).execute(request);

        var captured = ArgumentCaptor.forClass(CreateOrderCommand.class);
        verify(gateway, times(2)).createOrder(captured.capture(), any(UUID.class));
        var first = captured.getAllValues().get(0);
        var retry = captured.getAllValues().get(1);
        assertThat(first.externalReference()).isEqualTo(paymentId.toString()).isEqualTo(retry.externalReference());
        assertThat(first.idempotencyKey()).isEqualTo("same-key").isEqualTo(retry.idempotencyKey());
        assertThat(first.expirationTime()).isEqualTo("PT9M30S");
        assertThat(retry.expirationTime()).isEqualTo("PT9M");
        assertThat(retry).usingRecursiveComparison().ignoringFields("expirationTime").isEqualTo(first);
    }
}
