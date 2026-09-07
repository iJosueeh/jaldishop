package com.jaldishop.backend.ordering.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrderDeliveryValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrderDelivery {

    String message() default "La dirección es obligatoria para pedidos con entrega a domicilio";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
