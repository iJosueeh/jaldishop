package com.jaldishop.backend.shared.security.ratelimit;

import com.jaldishop.backend.identity.web.dto.LoginRequest;
import com.jaldishop.backend.shared.exception.ApiError;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Locale;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class LoginRateLimitAdvice extends RequestBodyAdviceAdapter {
    private final RateLimits limits;
    public LoginRateLimitAdvice(RateLimits limits) { this.limits = limits; }

    @Override
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return type == LoginRequest.class;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage message, MethodParameter parameter,
                                Type type, Class<? extends HttpMessageConverter<?>> converter) {
        var login = (LoginRequest) body;
        if (login.email() != null)
            limits.check("login-account", login.email().trim().toLowerCase(Locale.ROOT), 5, Duration.ofMinutes(1));
        return body;
    }

    @ExceptionHandler(RateLimitExceeded.class)
    public ResponseEntity<ApiError> exceeded(RateLimitExceeded exception) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(exception.retryAfter()))
                .body(new ApiError("RATE_LIMIT_EXCEEDED", exception.getMessage()));
    }
}
