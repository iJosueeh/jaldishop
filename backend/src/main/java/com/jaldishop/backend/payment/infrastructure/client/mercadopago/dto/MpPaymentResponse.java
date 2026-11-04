package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpPaymentResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("status") String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("currency_id") String currencyId,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        @JsonProperty("payment_method_id") String paymentMethodId,
        @JsonProperty("payment_type_id") String paymentTypeId,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("date_approved") String dateApproved,
        @JsonProperty("date_created") String dateCreated,
        @JsonProperty("date_last_updated") String dateLastUpdated,
        @JsonProperty("collector_id") Long collectorId,
        @JsonProperty("live_mode") Boolean liveMode
) {}
