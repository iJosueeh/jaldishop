package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpOrderResponse(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("status") String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("processing_mode") String processingMode,
        @JsonProperty("capture_mode") String captureMode,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        @JsonProperty("total_paid_amount") BigDecimal totalPaidAmount,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("checkout_url") String checkoutUrl,
        @JsonProperty("transactions") MpTransactions transactions,
        @JsonProperty("collector_id") Long collectorId,
        @JsonProperty("live_mode") Boolean liveMode,
        @JsonProperty("date_created") String dateCreated
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpTransactions(
            @JsonProperty("payments") List<MpOrderPayment> payments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MpOrderPayment(
            @JsonProperty("id") Object id,
            @JsonProperty("status") String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("payment_type_id") String paymentTypeId,
            @JsonProperty("date_approved") String dateApproved
    ) {}

}
