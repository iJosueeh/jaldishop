package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StompDeliveryConfigurationTest {
    @Configuration
    @EnableWebMvc
    @Import({WebSocketConfig.class, StompAuthChannelInterceptor.class, StompDeliveryAuthorizationInterceptor.class})
    static class Config {
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean StoreContextService stores() { return mock(StoreContextService.class); }
    }

    @Test
    void registersDeliveryInterceptorOnRealOutboundChannel() {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Config.class);
            context.refresh();
            var outbound = (AbstractMessageChannel) context.getBean("clientOutboundChannel");
            assertThat(outbound.getInterceptors()).anyMatch(StompDeliveryAuthorizationInterceptor.class::isInstance);
        }
    }
}
