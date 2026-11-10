package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Set;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageChannel messageChannel;

    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtService, storeContextService, userRepository);
    }

    @Test
    @DisplayName("CONNECT: Debe autenticar exitosamente cuando se proporciona un Bearer token válido")
    void connectWithValidBearerTokenAuthenticatesSuccessfully() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        when(jwtService.getPrincipal("valid-token")).thenReturn(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof Authentication);
        Authentication auth = (Authentication) resultAccessor.getUser();
        assertTrue(auth.getPrincipal() instanceof StompPrincipal);
        StompPrincipal stompPrincipal = (StompPrincipal) auth.getPrincipal();
        assertEquals(userId.toString(), stompPrincipal.getName());
        assertEquals(principal, stompPrincipal.jwtPrincipal());
    }

    @Test
    @DisplayName("CONNECT: Debe autenticar cuando el token viene en el header passcode de STOMP")
    void connectWithPasscodeBearerTokenAuthenticatesSuccessfully() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        when(jwtService.getPrincipal("passcode-token")).thenReturn(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setPasscode("Bearer passcode-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
    }

    @Test
    @DisplayName("CONNECT: Debe rechazar la conexión si falta la cabecera Authorization")
    void connectWithoutAuthorizationHeaderThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("SUBSCRIBE: Permite al comerciante suscribirse a los pedidos de su propia tienda")
    void subscribeStoreOrdersByStoreOwnerIsPermitted() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/stores/" + storeId + "/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        doNothing().when(storeContextService).validateStoreOwnership(eq(storeId), eq(principal));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
        verify(storeContextService).validateStoreOwnership(storeId, principal);
    }

    @Test
    @DisplayName("SUBSCRIBE: Deniega suscripción a pedidos de tienda si el comerciante no es el dueño")
    void subscribeStoreOrdersByNonOwnerThrowsAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/stores/" + storeId + "/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        doThrow(new AccessDeniedException("No tiene permisos sobre esta tienda"))
                .when(storeContextService).validateStoreOwnership(eq(storeId), eq(principal));

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("SUBSCRIBE: Deniega suscripción a pedidos de tienda a usuarios sin rol MERCHANT")
    void subscribeStoreOrdersByCustomerThrowsAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/stores/" + storeId + "/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
        verifyNoInteractions(storeContextService);
    }

    @Test
    @DisplayName("SUBSCRIBE: Permite a un cliente suscribirse a su cola personal /user/queue/orders")
    void subscribeUserQueueOrdersByCustomerIsPermitted() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/user/queue/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("SUBSCRIBE: Deniega suscripción a /user/queue/orders a usuarios sin rol CUSTOMER")
    void subscribeUserQueueOrdersByNonCustomerThrowsAccessDenied() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/user/queue/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("SUBSCRIBE: Deniega cualquier destino fuera de la lista blanca permitida")
    void subscribeUnauthorizedDestinationThrowsAccessDenied() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("ADMIN"));
        Authentication auth = createAuthentication(principal);
        allow(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(auth);
        accessor.setDestination("/topic/broadcast/all");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    @DisplayName("SEND: Bloquea cualquier comando de negocio enviado por el cliente vía WebSocket")
    void sendCommandByClientIsBlocked() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        Authentication auth = createAuthentication(principal);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setUser(auth);
        accessor.setDestination("/app/orders/create");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(AccessDeniedException.class, () ->
                interceptor.preSend(message, messageChannel)
        );
    }

    @Test
    void revokedMerchantCannotConnectOrSubscribeWithOldRoles() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal oldPrincipal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        JwtPrincipal currentPrincipal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        when(jwtService.getPrincipal("old-token")).thenReturn(oldPrincipal);
        allow(currentPrincipal);
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization", "Bearer old-token");
        Message<?> result = interceptor.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), messageChannel);
        Authentication connected = (Authentication) StompHeaderAccessor.wrap(result).getUser();
        assertEquals(Set.of("CUSTOMER"), ((StompPrincipal) connected.getPrincipal()).jwtPrincipal().roles());

        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(createAuthentication(oldPrincipal));
        subscribe.setDestination("/topic/stores/" + UUID.randomUUID() + "/orders");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders());
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, messageChannel));
        verifyNoInteractions(storeContextService);
    }

    @Test
    void passwordResetRejectsOldConnectAndExistingSubscription() {
        UUID id = UUID.randomUUID();
        JwtPrincipal old = new JwtPrincipal(id, Set.of("CUSTOMER"));
        User user = User.create("a@example.com", "encoded", "Test", "User", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));
        user.resetPassword("new-encoded");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.getPrincipal("old-token")).thenReturn(old);
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization", "Bearer old-token");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), messageChannel));
        StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(createAuthentication(old)); subscribe.setDestination("/user/queue/orders");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], subscribe.getMessageHeaders()), messageChannel));
    }

    private void allow(JwtPrincipal principal) {
        var roles = principal.roles().stream().map(role -> new Role((short) 1, RoleName.valueOf(role)))
                .collect(java.util.stream.Collectors.toSet());
        User user = User.reconstitute(principal.userId(), "test@example.com", "hash", "Test", "User", null,
                UserStatus.ACTIVE, roles, Instant.now(), Instant.now());
        when(userRepository.findById(principal.userId())).thenReturn(Optional.of(user));
    }

    private Authentication createAuthentication(JwtPrincipal principal) {
        StompPrincipal stompPrincipal = new StompPrincipal(principal.userId().toString(), principal);
        return new UsernamePasswordAuthenticationToken(stompPrincipal, null, java.util.List.of());
    }

}
