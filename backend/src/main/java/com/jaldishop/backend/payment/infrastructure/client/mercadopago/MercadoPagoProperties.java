package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

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
     * Clave secreta para validación de la firma criptográfica x-signature del webhook.
     */
    private String webhookSecret = "";

    /**
     * ID de la cuenta receptora del comercio (collector_id) esperada en Mercado Pago.
     * Si está configurado, se valida en cada notificación de pago.
     */
    private Long expectedCollectorId;

    /**
     * Entorno esperado (true para producción, false para sandbox/test).
     * Si está configurado, se valida en cada notificación de pago.
     */
    private Boolean expectedLiveMode;

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
