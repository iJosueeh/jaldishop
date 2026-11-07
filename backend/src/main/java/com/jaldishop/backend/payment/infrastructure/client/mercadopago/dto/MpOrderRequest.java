package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MpOrderRequest(
        @JsonProperty("type") String type,
        @JsonProperty("processing_mode") String processingMode,
        @JsonProperty("capture_mode") String captureMode,
        @JsonProperty("total_amount") @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("items") List<MpOrderItem> items,
        @JsonProperty("payer") MpOrderPayer payer,
        @JsonProperty("config") MpOrderConfig config,
        @JsonProperty("expiration_time") String expirationTime
) {

    public MpOrderRequest(
            String type,
            String processingMode,
            String captureMode,
            BigDecimal totalAmount,
            String externalReference,
            List<MpOrderItem> items,
            MpOrderPayer payer,
            MpOrderConfig config
    ) {
        this(type, processingMode, captureMode, totalAmount, externalReference, items, payer, config, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MpOrderItem(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("unit_price") @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal unitPrice
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MpOrderPayer(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("email") String email,
            @JsonProperty("phone") MpOrderPhone phone
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MpOrderPhone(
            @JsonProperty("area_code") String areaCode,
            @JsonProperty("number") String number
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MpOrderConfig(
            @JsonProperty("online") MpOnlineConfig online
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MpOnlineConfig(
            @JsonProperty("success_url") String successUrl,
            @JsonProperty("failure_url") String failureUrl,
            @JsonProperty("pending_url") String pendingUrl,
            @JsonProperty("auto_return") String autoReturn
    ) {}

}
