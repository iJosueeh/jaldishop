package com.jaldishop.backend.realtime.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompDeliveryAuthorizationInterceptor implements ExecutorChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StompDeliveryAuthorizationInterceptor.class);
    private final ObjectProvider<SimpUserRegistry> users;
    private final StompAuthChannelInterceptor authorization;

    public StompDeliveryAuthorizationInterceptor(ObjectProvider<SimpUserRegistry> users, StompAuthChannelInterceptor authorization) {
        this.users = users;
        this.authorization = authorization;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        var headers = message.getHeaders();
        if (SimpMessageHeaderAccessor.getMessageType(headers) != SimpMessageType.MESSAGE) {
            return message;
        }
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
        String destination = SimpMessageHeaderAccessor.getDestination(headers);
        if (sessionId == null || subscriptionId == null || destination == null) {
            return null;
        }

        try {
            var subscriptions = users.getObject().findSubscriptions(subscription ->
                    sessionId.equals(subscription.getSession().getId()) && subscriptionId.equals(subscription.getId()));
            if (subscriptions.size() != 1) {
                return null;
            }
            var subscription = subscriptions.iterator().next();
            String requested = subscription.getDestination();
            boolean matches = requested.equals(destination)
                    || (requested.equals("/user/queue/orders") && destination.equals("/queue/orders-user" + sessionId));
            var principal = subscription.getSession().getUser().getPrincipal();
            if (!matches || !(principal instanceof Authentication auth)
                    || !(auth.getPrincipal() instanceof StompPrincipal stompPrincipal)) {
                return null;
            }
            authorization.authorize(stompPrincipal.jwtPrincipal(), requested);
            return message;
        } catch (RuntimeException exception) {
            log.warn("Entrega STOMP bloqueada: no se pudo validar la autorización actual.");
            return null;
        }
    }
}
