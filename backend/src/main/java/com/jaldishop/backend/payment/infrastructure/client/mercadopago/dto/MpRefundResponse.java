package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpRefundResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("status") String status,
        @JsonProperty("date_created") String dateCreated,
        @JsonProperty("refund_mode") String refundMode,
        @JsonProperty("reason") String reason
) {
}
