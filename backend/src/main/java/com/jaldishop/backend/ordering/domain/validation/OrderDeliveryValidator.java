package com.jaldishop.backend.ordering.domain.validation;

import com.jaldishop.backend.ordering.application.dto.CreateOrderRequest;
import com.jaldishop.backend.ordering.domain.DeliveryMode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrderDeliveryValidator implements ConstraintValidator<ValidOrderDelivery, CreateOrderRequest> {

    @Override
    public boolean isValid(CreateOrderRequest request, ConstraintValidatorContext context) {
        if (request == null || request.deliveryMode() == null) {
            return true;
        }

        if (request.deliveryMode() == DeliveryMode.DELIVERY) {
            if (request.deliveryAddress() == null || request.deliveryAddress().isBlank()) {
                context.disableDefaultConstraintViolation();

                context.buildConstraintViolationWithTemplate("La dirección de entrega es obligatoria para modalidad DELIVERY")
                        .addPropertyNode("deliveryAddress")
                        .addConstraintViolation();

                return false;
            }
        }

        return true;
    }
}
