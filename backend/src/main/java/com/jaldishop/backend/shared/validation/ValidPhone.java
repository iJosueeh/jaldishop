package com.jaldishop.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {

    String message() default "El número de teléfono no tiene un formato válido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
