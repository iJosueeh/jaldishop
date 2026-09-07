package com.jaldishop.backend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+51)?9\\d{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return PHONE_PATTERN.matcher(value.trim()).matches();
    }
}
