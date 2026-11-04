package com.jaldishop.backend.payment.infrastructure.client.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpPreferenceResponse(
        @JsonProperty("id") String id,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("sandbox_init_point") String sandboxInitPoint,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("collector_id") Long collectorId,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("date_created") String dateCreated
) {}
