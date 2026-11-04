package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MpPreferenceRequest(
        @JsonProperty("items") List<MpItem> items,
        @JsonProperty("payer") MpPayer payer,
        @JsonProperty("back_urls") MpBackUrls backUrls,
        @JsonProperty("auto_return") String autoReturn,
        @JsonProperty("notification_url") String notificationUrl,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("statement_descriptor") String statementDescriptor
) {

    public record MpItem(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("currency_id") String currencyId
    ) {}

    public record MpPayer(
            @JsonProperty("name") String name,
            @JsonProperty("surname") String surname,
            @JsonProperty("email") String email,
            @JsonProperty("phone") MpPhone phone
    ) {}

    public record MpPhone(
            @JsonProperty("area_code") String areaCode,
            @JsonProperty("number") String number
    ) {}

    public record MpBackUrls(
            @JsonProperty("success") String success,
            @JsonProperty("pending") String pending,
            @JsonProperty("failure") String failure
    ) {}

}
