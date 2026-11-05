package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapeo de la respuesta oficial del endpoint POST /v1/orders/{order_id}/refund.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MpOrderRefundResponse(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("transactions") MpRefundTransactions transactions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpRefundTransactions(
            @JsonProperty("refunds") List<MpRefundItem> refunds
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpRefundItem(
            @JsonProperty("id") Object id,
            @JsonProperty("payment_id") Object paymentId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("status") String status,
            @JsonProperty("date_created") String dateCreated
    ) {}

}
