package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades configurables para el cliente HTTP de Mercado Pago.
 */
@Component
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    /**
     * URL base de la API de Mercado Pago.
     */
    private String baseUrl = "https://api.mercadopago.com";

    /**
     * Token de acceso (Bearer token).
     */
    private String accessToken = "";

    /**
     * Timeout de conexión en milisegundos.
     */
    private int connectTimeoutMs = 5000;

    /**
     * Timeout de lectura en milisegundos.
     */
    private int readTimeoutMs = 10000;

    /**
     * URL del webhook donde Mercado Pago notifica eventos de pago.
     */
    private String notificationUrl = "https://api.jaldishop.com/api/v1/checkout/mercado-pago/webhook";

    /**
     * URLs de retorno al comercio tras finalizar o cancelar en Checkout Pro.
     */
    private String backUrlSuccess = "https://jaldishop.com/checkout/success";
    private String backUrlFailure = "https://jaldishop.com/checkout/failure";
    private String backUrlPending = "https://jaldishop.com/checkout/pending";

    /**
     * Modo de integración de checkout de Mercado Pago (PREFERENCES u ORDERS).
     * Predeterminado: PREFERENCES para compatibilidad garantizada.
     */
    private CheckoutIntegrationMode checkoutMode = CheckoutIntegrationMode.PREFERENCES;

    public enum CheckoutIntegrationMode {
        PREFERENCES,
        ORDERS
    }

    public CheckoutIntegrationMode getCheckoutMode() {
        return checkoutMode;
    }

    public void setCheckoutMode(CheckoutIntegrationMode checkoutMode) {
        this.checkoutMode = checkoutMode;
    }

    /**
     * Clave secreta para validación de la firma criptográfica x-signature del webhook.
     */
    private String webhookSecret = "";

    /**
     * ID de la cuenta receptora del comercio (collector_id) esperada en Mercado Pago.
     * Si está configurado, se valida en cada notificación de pago.
     */
    private Long expectedCollectorId;

    /**
     * Interruptor de emergencia para habilitar o deshabilitar la creación de nuevos checkouts.
     * Predeterminado: true. Si es false, rechaza nuevos checkouts pero mantiene operativo el procesamiento de webhooks.
     */
    private boolean checkoutEnabled = true;

    /**
     * Entorno esperado (estrictamente false para sandbox/test).
     * Política fail-closed: este entorno rechaza true o null en arranque y en tiempo de ejecución.
     */
    private Boolean expectedLiveMode;

    @PostConstruct
    public void validateEnvironmentSafety() {
        if (!Boolean.FALSE.equals(this.expectedLiveMode)) {
            throw new IllegalStateException("FAIL-CLOSED: Entorno actual configurado exclusivamente para pruebas. "
                    + "mercadopago.expected-live-mode debe ser estrictamente false (recibido: " + this.expectedLiveMode + "). "
                    + "Arranque abortado para impedir cobros productivos accidentales.");
        }
    }

    public boolean isCheckoutEnabled() {
        return checkoutEnabled;
    }

    public void setCheckoutEnabled(boolean checkoutEnabled) {
        this.checkoutEnabled = checkoutEnabled;
    }

    public Long getExpectedCollectorId() {
        return expectedCollectorId;
    }

    public void setExpectedCollectorId(Long expectedCollectorId) {
        this.expectedCollectorId = expectedCollectorId;
    }

    public Boolean getExpectedLiveMode() {
        return expectedLiveMode;
    }

    public void setExpectedLiveMode(Boolean expectedLiveMode) {
        if (!Boolean.FALSE.equals(expectedLiveMode)) {
            throw new IllegalArgumentException("FAIL-CLOSED: Entorno actual restringido exclusivamente a pruebas. "
                    + "mercadopago.expected-live-mode debe ser estrictamente false (recibido: " + expectedLiveMode + "). "
                    + "Valores null o true están prohibidos para evitar operaciones productivas.");
        }
        this.expectedLiveMode = expectedLiveMode;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public String getBackUrlSuccess() {
        return backUrlSuccess;
    }

    public void setBackUrlSuccess(String backUrlSuccess) {
        this.backUrlSuccess = backUrlSuccess;
    }

    public String getBackUrlFailure() {
        return backUrlFailure;
    }

    public void setBackUrlFailure(String backUrlFailure) {
        this.backUrlFailure = backUrlFailure;
    }

    public String getBackUrlPending() {
        return backUrlPending;
    }

    public void setBackUrlPending(String backUrlPending) {
        this.backUrlPending = backUrlPending;
    }

}
