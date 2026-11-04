package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

/**
 * Excepción específica para errores de comunicación o respuestas anómalas de Mercado Pago.
 */
public class MercadoPagoException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public MercadoPagoException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public MercadoPagoException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "MercadoPagoException{" +
                "statusCode=" + statusCode +
                ", errorCode='" + errorCode + '\'' +
                ", message=" + getMessage() +
                '}';
    }

}
