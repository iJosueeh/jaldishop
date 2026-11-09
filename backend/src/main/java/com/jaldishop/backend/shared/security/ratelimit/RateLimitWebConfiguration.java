package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitWebConfiguration implements WebMvcConfigurer {
    private final RateLimits limits;
    public RateLimitWebConfiguration(RateLimits limits) { this.limits = limits; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor());
    }

    HandlerInterceptor interceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                // Only the servlet peer is trusted. Forwarded headers require verified proxy configuration.
                String ip = request.getRemoteAddr();
                if ("POST".equals(request.getMethod())) {
                    if (path.equals("/api/v1/auth/login"))
                        limits.check("login-ip", ip, 30, Duration.ofMinutes(1));
                    if (path.equals("/api/v1/auth/register") || path.equals("/api/v1/auth/register/merchant"))
                        limits.check("register-ip", ip, 3, Duration.ofMinutes(10));
                }
                if ("GET".equals(request.getMethod()) &&
                        (path.equals("/api/v1/stores/search") || path.equals("/api/v1/stores/featured")))
                    limits.check("search-ip", ip, 60, Duration.ofMinutes(1));
                return true;
            }
        };
    }
}
