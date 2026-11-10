package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MercadoPagoPropertiesTest {

    @Test
    @DisplayName("Por defecto checkoutEnabled es true y checkoutMode es PREFERENCES")
    void shouldHaveSafeDefaults() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        assertThat(properties.getExpectedLiveMode()).isNull();
        assertThat(properties.isCheckoutEnabled()).isTrue();
        assertThat(properties.getCheckoutMode()).isEqualTo(MercadoPagoProperties.CheckoutIntegrationMode.PREFERENCES);
    }

    @Test
    @DisplayName("setExpectedLiveMode acepta false")
    void shouldAcceptFalseExpectedLiveMode() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setExpectedLiveMode(false);
        assertThat(properties.getExpectedLiveMode()).isFalse();
    }

    @Test
    @DisplayName("setExpectedLiveMode rechaza true con IllegalArgumentException")
    void shouldRejectTrueExpectedLiveMode() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        assertThatThrownBy(() -> properties.setExpectedLiveMode(true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAIL-CLOSED");
    }

    @Test
    @DisplayName("setExpectedLiveMode rechaza null con IllegalArgumentException")
    void shouldRejectNullExpectedLiveMode() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        assertThatThrownBy(() -> properties.setExpectedLiveMode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAIL-CLOSED");
    }

    @Test
    @DisplayName("validateEnvironmentSafety falla en arranque si expectedLiveMode es null")
    void shouldFailValidationWhenExpectedLiveModeIsNull() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        assertThatThrownBy(properties::validateEnvironmentSafety)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAIL-CLOSED");
    }

    @Test
    @DisplayName("validateEnvironmentSafety pasa en arranque si expectedLiveMode=false")
    void shouldPassValidationWhenExpectedLiveModeIsFalse() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setExpectedLiveMode(false);
        properties.validateEnvironmentSafety();
        assertThat(properties.getExpectedLiveMode()).isFalse();
    }

    @Test
    @DisplayName("checkoutEnabled puede deshabilitarse como interruptor de emergencia")
    void shouldToggleCheckoutEnabled() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setCheckoutEnabled(false);
        assertThat(properties.isCheckoutEnabled()).isFalse();
    }
}
