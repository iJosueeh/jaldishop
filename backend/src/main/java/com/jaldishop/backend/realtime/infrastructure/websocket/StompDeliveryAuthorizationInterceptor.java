package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompDeliveryAuthorizationInterceptor implements ExecutorChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StompDeliveryAuthorizationInterceptor.class);
    private final ObjectProvider<SimpUserRegistry> users;
    private final StompAuthChannelInterceptor authorization;
    private final Map<String, JwtPrincipal> sessionPrincipals = new ConcurrentHashMap<>();

    public StompDeliveryAuthorizationInterceptor(ObjectProvider<SimpUserRegistry> users, StompAuthChannelInterceptor authorization) {
        this.users = users;
        this.authorization = authorization;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sessionPrincipals.remove(event.getSessionId());
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        var headers = message.getHeaders();
        var type = SimpMessageHeaderAccessor.getMessageType(headers);
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        if (type == SimpMessageType.CONNECT_ACK && sessionId != null) {
            // SimpUserRegistry keeps the first user's principal across all of their sessions.
            var principal = SimpMessageHeaderAccessor.getUser(headers);
            if (principal instanceof Authentication auth && auth.getPrincipal() instanceof StompPrincipal stompPrincipal) {
                sessionPrincipals.put(sessionId, stompPrincipal.jwtPrincipal());
            } else {
                sessionPrincipals.remove(sessionId);
            }
        } else if (type == SimpMessageType.DISCONNECT_ACK && sessionId != null) {
            sessionPrincipals.remove(sessionId);
        }
        if (type != SimpMessageType.MESSAGE) {
            return message;
        }
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
            JwtPrincipal principal = sessionPrincipals.get(sessionId);
            if (!matches || principal == null
                    || !principal.userId().toString().equals(subscription.getSession().getUser().getName())) {
                return null;
            }
            authorization.authorize(principal, requested);
            return message;
        } catch (RuntimeException exception) {
            log.warn("Entrega STOMP bloqueada: no se pudo validar la autorización actual.");
            return null;
        }
    }
}
