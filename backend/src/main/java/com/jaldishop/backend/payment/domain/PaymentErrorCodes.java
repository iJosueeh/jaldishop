package com.jaldishop.backend.payment.domain;

public final class PaymentErrorCodes {

    private PaymentErrorCodes() {
    }

    public static final String RN_PAY_02 = "RN-PAY-02";
    public static final String PAYMENT_GATEWAY_ERROR = "PAYMENT_GATEWAY_ERROR";
    public static final String PAYMENT_GATEWAY_TIMEOUT = "PAYMENT_GATEWAY_TIMEOUT";
    public static final String PAYMENT_RATE_LIMIT = "PAYMENT_RATE_LIMIT";
    public static final String PAYMENT_ALREADY_APPROVED = "PAYMENT_ALREADY_APPROVED";
    public static final String PAYMENT_IN_PROGRESS = "PAYMENT_IN_PROGRESS";
    public static final String INVALID_WEBHOOK_SIGNATURE = "INVALID_WEBHOOK_SIGNATURE";
    public static final String BAD_WEBHOOK_REQUEST = "BAD_WEBHOOK_REQUEST";
}
