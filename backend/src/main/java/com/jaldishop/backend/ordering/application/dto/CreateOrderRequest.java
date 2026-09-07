package com.jaldishop.backend.ordering.application.dto;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.validation.ValidOrderDelivery;
import jakarta.validation.constraints.NotNull;

@ValidOrderDelivery
public record CreateOrderRequest(
   @NotNull(message = "La modalidad de entrega es obligatoria")
   DeliveryMode deliveryMode,

   String deliveryAddress
) {}
