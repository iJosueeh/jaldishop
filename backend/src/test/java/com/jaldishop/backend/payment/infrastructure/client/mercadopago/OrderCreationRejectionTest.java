package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class OrderCreationRejectionTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String rejected = "{\"errors\":[{\"code\":\"invalid_total_amount\"}]}";

    @Test void onlyExactDocumentedValidationIsConfirmed() {
        assertThat(OrderCreationRejection.isInvalidTotalAmount(400, rejected, mapper)).isTrue();
        assertThat(OrderCreationRejection.isConfirmed(new MercadoPagoException(400,
                OrderCreationRejection.INVALID_TOTAL_AMOUNT, "safe"))).isTrue();
        for (int status : new int[]{401, 403, 404, 409, 422, 429, 500, 504, 0}) {
            assertThat(OrderCreationRejection.isInvalidTotalAmount(status, rejected, mapper)).isFalse();
        }
    }

    @Test void ambiguousAndSensitiveBodiesNeverBecomeEvidence() {
        for (String body : new String[]{"", "not json", "null", "{}",
                "{\"message\":\"invalid_total_amount\"}",
                "{\"errors\":[{\"code\":\"unknown\"}]}",
                "{\"errors\":[{\"code\":\"invalid_total_amount\"},{\"code\":\"other\"}]}",
                "{\"id\":\"ORD-1\",\"errors\":[{\"code\":\"invalid_total_amount\"}]}"}) {
            assertThat(OrderCreationRejection.isInvalidTotalAmount(400, body, mapper)).isFalse();
        }
        assertThat(OrderCreationRejection.isConfirmed(new MercadoPagoException(400, "MP_BAD_REQUEST", rejected))).isFalse();
    }
}
