package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern STORE_ORDERS_PATTERN =
            Pattern.compile("^/topic/stores/([0-9a-fA-F\\-]{36})/orders$");

    private static final String USER_QUEUE_ORDERS = "/user/queue/orders";

    private final JwtService jwtService;
    private final StoreContextService storeContextService;
    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(JwtService jwtService, StoreContextService storeContextService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.storeContextService = storeContextService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(message, accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            String passcode = accessor.getPasscode();
            if (passcode != null && passcode.startsWith("Bearer ")) {
                authHeader = passcode;
            }
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Token JWT no proporcionado en la conexión STOMP (se requiere cabecera 'Authorization: Bearer <token>')."
            );
        }

        String token = authHeader.substring(7).trim();
        JwtPrincipal jwtPrincipal = currentPrincipal(jwtService.getPrincipal(token).userId());

        var authorities = jwtPrincipal.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        StompPrincipal stompPrincipal = new StompPrincipal(jwtPrincipal.userId().toString(), jwtPrincipal);
        var authentication = new UsernamePasswordAuthenticationToken(stompPrincipal, null, authorities);

        if (accessor.isMutable()) {
            accessor.setUser(authentication);
            return message;
        } else {
            StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
            mutableAccessor.setUser(authentication);
            return org.springframework.messaging.support.MessageBuilder
                    .createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (!(user instanceof Authentication auth) || !(auth.getPrincipal() instanceof StompPrincipal stompPrincipal)) {
            throw new AccessDeniedException("Sesión no autenticada para realizar suscripción STOMP.");
        }

        authorize(stompPrincipal.jwtPrincipal().userId(), destination);
    }

    void authorize(UUID userId, String destination) {
        JwtPrincipal jwtPrincipal = currentPrincipal(userId);

        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Destino de suscripción no proporcionado.");
        }

        Matcher storeMatcher = STORE_ORDERS_PATTERN.matcher(destination);
        if (storeMatcher.matches()) {
            handleStoreOrdersSubscribe(storeMatcher.group(1), jwtPrincipal);
            return;
        }

        if (USER_QUEUE_ORDERS.equals(destination)) {
            handleUserQueueSubscribe(jwtPrincipal);
            return;
        }

        throw new AccessDeniedException("Destino de suscripción no autorizado: " + destination);
    }

    private void handleStoreOrdersSubscribe(String storeIdStr, JwtPrincipal jwtPrincipal) {
        if (!jwtPrincipal.roles().contains("MERCHANT")) {
            throw new AccessDeniedException("Solo usuarios con rol MERCHANT pueden suscribirse a eventos de tienda.");
        }

        UUID storeId;
        try {
            storeId = UUID.fromString(storeIdStr);
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Identificador de tienda no válido en la suscripción.");
        }

        storeContextService.validateStoreOwnership(storeId, jwtPrincipal);
    }

    private void handleUserQueueSubscribe(JwtPrincipal jwtPrincipal) {
        if (!jwtPrincipal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo usuarios con rol CUSTOMER pueden suscribirse a la cola personal de pedidos.");
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        throw new AccessDeniedException("No se permiten comandos de negocio mediante WebSocket. Utilice los endpoints HTTP REST oficiales.");
    }

    private JwtPrincipal currentPrincipal(UUID userId) {
        var user = userRepository.findById(userId)
                .filter(u -> u.canAuthenticate())
                .orElseThrow(() -> new AccessDeniedException("Usuario no autorizado."));
        var roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(java.util.stream.Collectors.toSet());
        return new JwtPrincipal(userId, roles);
    }

}
