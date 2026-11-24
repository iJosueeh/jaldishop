package com.jaldishop.backend.realtime.infrastructure.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final StompDeliveryAuthorizationInterceptor deliveryAuthorizationInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           StompDeliveryAuthorizationInterceptor deliveryAuthorizationInterceptor,
                           @Value("${app.cors.allowed-origins:https://negocios-jaldishop.pages.dev,https://jaldishop.net,https://www.jaldishop.net,https://jaldishop.pe,https://www.jaldishop.pe}")
                           String allowedOrigins) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.deliveryAuthorizationInterceptor = deliveryAuthorizationInterceptor;
        this.allowedOrigins = com.jaldishop.backend.identity.infrastructure.security.WebCorsConfiguration.parseOrigins(allowedOrigins);
    }

    public List<String> getAllowedOrigins() {
        return List.copyOf(allowedOrigins);
    }

    @Bean(name = "wsMessageBrokerTaskScheduler")
    public TaskScheduler wsMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(wsMessageBrokerTaskScheduler());

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(10 * 1000);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(deliveryAuthorizationInterceptor);
    }

}
