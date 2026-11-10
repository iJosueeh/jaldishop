package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

/**
 * Excepción de seguridad lanzada cuando una orden creada en Mercado Pago
 * presenta una discrepancia crítica de entorno (live_mode != false).
 * Preserva el ID remoto para auditoría e investigación sin exponer secretos.
 */
public class MercadoPagoOrderSecurityException extends SecurityException {

    private final String remoteOrderId;
    private final Boolean remoteLiveMode;

    public MercadoPagoOrderSecurityException(String message, String remoteOrderId, Boolean remoteLiveMode) {
        super(message);
        this.remoteOrderId = remoteOrderId;
        this.remoteLiveMode = remoteLiveMode;
    }

    public String getRemoteOrderId() {
        return remoteOrderId;
    }

    public Boolean getRemoteLiveMode() {
        return remoteLiveMode;
    }
}
