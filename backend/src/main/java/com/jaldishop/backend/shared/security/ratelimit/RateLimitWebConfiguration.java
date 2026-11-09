package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitWebConfiguration implements WebMvcConfigurer {
    private final RateLimits limits;
    private final ClientIpResolver clientIp;
    public RateLimitWebConfiguration(RateLimits limits, Environment environment) {
        this.limits = limits;
        this.clientIp = new ClientIpResolver(environment);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor());
    }

    HandlerInterceptor interceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String path = request.getRequestURI().substring(request.getContextPath().length());

                if ("POST".equals(request.getMethod())) {
                    if (path.equals("/api/v1/auth/login"))
                        limits.check("login-ip", clientIp.resolve(request), 30, Duration.ofMinutes(1));
                    if (path.equals("/api/v1/auth/register") || path.equals("/api/v1/auth/register/merchant"))
                        limits.check("register-ip", clientIp.resolve(request), 3, Duration.ofMinutes(10));
                }
                if ("GET".equals(request.getMethod()) &&
                        (path.equals("/api/v1/stores/search") || path.equals("/api/v1/stores/featured")))
                    limits.check("search-ip", clientIp.resolve(request), 60, Duration.ofMinutes(1));
                return true;
            }
        };
    }
}
