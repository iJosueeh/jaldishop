package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import tools.jackson.databind.ObjectMapper;

/** Narrow allowlist; generic 4xx, conflicts, rate limits and malformed bodies remain unknown. */
public final class OrderCreationRejection {
    public static final String INVALID_TOTAL_AMOUNT = "MP_ORDER_INVALID_TOTAL_AMOUNT";

    private OrderCreationRejection() {}

    // Documented request validation: a zero total is rejected before creating an order.
    // https://www.mercadopago.com.pe/developers/en/reference/online-payments/checkout-pro/create-order/post
    public static boolean isInvalidTotalAmount(int status, String body, ObjectMapper mapper) {
        if (status != 400) return false;
        try {
            var root = mapper.readTree(body);
            if (root == null || root.has("id")) return false;
            var errors = root.path("errors");
            return errors.isArray() && errors.size() == 1
                    && "invalid_total_amount".equals(errors.get(0).path("code").asText());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isConfirmed(MercadoPagoException failure) {
        return failure.getStatusCode() == 400 && INVALID_TOTAL_AMOUNT.equals(failure.getErrorCode());
    }
}
