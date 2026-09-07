package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.web.validation.ValidOrderDelivery;
import jakarta.validation.constraints.NotNull;

@ValidOrderDelivery
public record CreateOrderRequest(
   @NotNull(message = "La modalidad de entrega es obligatoria")
   DeliveryMode deliveryMode,

   String deliveryAddress
) {}
