package com.jaldishop.backend.shared.util;

import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Reproduce en tests MockMvc standalone la validación de parámetros que Spring Boot
 * registra en producción vía ValidationAutoConfiguration: un controller construido a mano
 * no pasa por los BeanPostProcessor del contexto, por lo que sin este proxy las
 * restricciones {@code @Min/@Max/@Size} sobre {@code @RequestParam}/{@code @PathVariable}
 * no se aplicarían y el handler de ConstraintViolationException quedaría sin ejecutarse.
 */
public final class MethodValidationTestSupport {

    private MethodValidationTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T withParameterValidation(T controller) {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.setProxyTargetClass(true);
        postProcessor.afterPropertiesSet();
        return (T) postProcessor.postProcessAfterInitialization(controller, "controller");
    }
}
