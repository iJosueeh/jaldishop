package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.CloseStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StompDeliveryAuthorizationBrokerTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UserRepository users = mock(UserRepository.class);
    private final StoreContextService stores = mock(StoreContextService.class);
    private final JwtService jwt = mock(JwtService.class);
    private final DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();
    private final ExecutorSubscribableChannel inbound = new ExecutorSubscribableChannel();
    private final ExecutorSubscribableChannel outbound = new ExecutorSubscribableChannel();
    private final ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
    private final List<Message<?>> delivered = new ArrayList<>();
    private final AtomicReference<User> current = new AtomicReference<>();
    private StompAuthChannelInterceptor inboundAuthorization;
    private StompDeliveryAuthorizationInterceptor deliveryAuthorization;
    private SimpleBrokerMessageHandler broker;
    private UserDestinationMessageHandler userDestinations;
    private Authentication principal;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(users.findById(userId)).thenAnswer(invocation -> Optional.ofNullable(current.get()));
        current.set(user(UserStatus.ACTIVE, RoleName.CUSTOMER, RoleName.MERCHANT));
        doAnswer(invocation -> {
            if (!storeId.equals(invocation.getArgument(0))) throw new AccessDeniedException("foreign");
            return null;
        }).when(stores).validateStoreOwnership(any(), any());
        inboundAuthorization = new StompAuthChannelInterceptor(jwt, stores, users);
        ObjectProvider<SimpUserRegistry> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(registry);
        deliveryAuthorization = new StompDeliveryAuthorizationInterceptor(provider, inboundAuthorization);
        outbound.addInterceptor(deliveryAuthorization);
        outbound.subscribe(message -> {
            if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) == SimpMessageType.MESSAGE) delivered.add(message);
        });
        broker = new SimpleBrokerMessageHandler(inbound, outbound, brokerChannel, List.of("/topic", "/queue"));
        broker.start();
        userDestinations = new UserDestinationMessageHandler(inbound, brokerChannel, new DefaultUserDestinationResolver(registry));
        userDestinations.start();
        JwtPrincipal jwtPrincipal = new JwtPrincipal(userId, Set.of("CUSTOMER", "MERCHANT"));
        principal = new UsernamePasswordAuthenticationToken(new StompPrincipal(userId.toString(), jwtPrincipal), null, List.of());
    }

    @Test
    void passwordResetStopsDeliveryToAlreadySubscribedSessions() {
        connect("password-reset-session");
        subscribe("password-reset-session", "merchant", topic());
        subscribe("password-reset-session", "customer", "/user/queue/orders");
        publish(topic()); publishCustomer();
        assertThat(delivered).hasSize(2); delivered.clear();
        current.get().resetPassword("new-encoded-password");
        publish(topic()); publishCustomer();
        assertThat(delivered).isEmpty();
    }

    @Test
    void newSessionReceivesAfterResetWhileOldSessionRemainsConnected() {
        connect("old-session");
        subscribe("old-session", "old-customer", "/user/queue/orders");
        publishCustomer();
        assertThat(delivered).hasSize(1);
        delivered.clear();

        current.get().resetPassword("new-encoded-password");
        JwtPrincipal fresh = new JwtPrincipal(userId, Set.of("CUSTOMER", "MERCHANT"),
                current.get().getCredentialsVersion());
        principal = new UsernamePasswordAuthenticationToken(new StompPrincipal(userId.toString(), fresh), null, List.of());
        connect("new-session");
        subscribe("new-session", "new-customer", "/user/queue/orders");

        publishCustomer();

        assertThat(delivered).hasSize(1);
        assertThat(SimpMessageHeaderAccessor.getSessionId(delivered.getFirst().getHeaders()))
                .isEqualTo("new-session");
        assertThat(SimpMessageHeaderAccessor.getSubscriptionId(delivered.getFirst().getHeaders()))
                .isEqualTo("new-customer");
    }

    @Test
    void existingSessionsStopReceivingAfterRoleRevocationAndOtherRoleContinues() {
        connect("s1");
        connect("s2");
        subscribe("s1", "merchant", topic());
        subscribe("s2", "merchant", topic());
        subscribe("s1", "customer", "/user/queue/orders");
        publish(topic());
        publishCustomer();
        assertThat(delivered).hasSize(3);
        delivered.clear();

        current.set(user(UserStatus.ACTIVE, RoleName.CUSTOMER));
        publish(topic());
        publishCustomer();
        assertThat(delivered).hasSize(1);
        assertThat(SimpMessageHeaderAccessor.getSubscriptionId(delivered.getFirst().getHeaders())).isEqualTo("customer");
        assertThatThrownBy(() -> subscribe("s1", "new", topic())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void disabledUserReceivesNeitherStoreNorPrivateMessages() {
        connect("s1");
        subscribe("s1", "merchant", topic());
        subscribe("s1", "customer", "/user/queue/orders");
        current.set(user(UserStatus.INACTIVE, RoleName.CUSTOMER, RoleName.MERCHANT));
        publish(topic());
        publishCustomer();
        assertThat(delivered).isEmpty();
    }

    @Test
    void customerRoleRemovalStopsBothPrivateSessionsButPreservesMerchantDelivery() {
        connect("s1");
        connect("s2");
        subscribe("s1", "customer", "/user/queue/orders");
        subscribe("s2", "customer", "/user/queue/orders");
        subscribe("s1", "merchant", topic());
        publishCustomer();
        assertThat(delivered).hasSize(2);
        delivered.clear();
        current.set(user(UserStatus.ACTIVE, RoleName.MERCHANT));
        publishCustomer();
        publish(topic());
        assertThat(delivered).hasSize(1);
        assertThat(SimpMessageHeaderAccessor.getSubscriptionId(delivered.getFirst().getHeaders())).isEqualTo("merchant");
    }

    @Test
    void transportDisconnectDropsDeliveryEvenBeforeUserRegistryCleansUp() {
        connect("disconnected-session");
        subscribe("disconnected-session", "customer", "/user/queue/orders");
        var disconnected = message(SimpMessageType.DISCONNECT, "disconnected-session", null, null, principal);
        var event = new SessionDisconnectEvent(this, disconnected, "disconnected-session", CloseStatus.NORMAL, principal);
        deliveryAuthorization.onDisconnect(event);
        deliveryAuthorization.onDisconnect(event);
        publishCustomer();
        assertThat(delivered).isEmpty();
    }

    @Test
    void disconnectAcknowledgementClearsSessionIdentity() {
        connect("disconnected-session");
        subscribe("disconnected-session", "customer", "/user/queue/orders");
        broker.handleMessage(message(SimpMessageType.DISCONNECT, "disconnected-session", null, null, principal));
        // The user registry is deliberately still stale; no delivery may use the discarded session.
        outbound.send(message(SimpMessageType.MESSAGE, "disconnected-session", "customer",
                "/queue/orders-userdisconnected-session", null));
        assertThat(delivered).isEmpty();
    }

    @Test
    void databaseFailureDropsDelivery() {
        connect("s1");
        subscribe("s1", "customer", "/user/queue/orders");
        when(users.findById(userId)).thenThrow(new IllegalStateException("unavailable"));
        publishCustomer();
        assertThat(delivered).isEmpty();
    }

    @Test
    void foreignDestinationAndLostOwnershipAreDenied() {
        connect("s1");
        String foreign = "/topic/stores/" + UUID.randomUUID() + "/orders";
        assertThatThrownBy(() -> subscribe("s1", "foreign", foreign)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> subscribe("s1", "foreign-user", "/user/" + UUID.randomUUID() + "/queue/orders"))
                .isInstanceOf(AccessDeniedException.class);
        subscribe("s1", "merchant", topic());
        doThrow(new AccessDeniedException("ownership revoked")).when(stores).validateStoreOwnership(eq(storeId), any());
        publish(topic());
        assertThat(delivered).isEmpty();
    }

    @Test
    void adminTopicStopsDeliveryWhenRoleRevokedOrSuspended() {
        current.set(userWithPrimary(UserStatus.ACTIVE, false, RoleName.ADMIN));
        JwtPrincipal adminPrincipal = new JwtPrincipal(userId, Set.of("ADMIN"), 0L, false);
        principal = new UsernamePasswordAuthenticationToken(new StompPrincipal(userId.toString(), adminPrincipal), null, List.of());

        connect("admin-session");
        subscribe("admin-session", "sub-admin-events", "/topic/admin/events");
        publish("/topic/admin/events");
        assertThat(delivered).hasSize(1);
        delivered.clear();

        // 1. Role revoked
        current.set(userWithPrimary(UserStatus.ACTIVE, false, RoleName.CUSTOMER));
        publish("/topic/admin/events");
        assertThat(delivered).isEmpty();

        // 2. Account suspended
        current.set(userWithPrimary(UserStatus.SUSPENDED, false, RoleName.ADMIN));
        publish("/topic/admin/events");
        assertThat(delivered).isEmpty();
    }

    @Test
    void operationalAdminCannotSubscribeToAdminUserQueueAndForeignUserCannotReceivePrivateQueue() {
        // Operational admin
        current.set(userWithPrimary(UserStatus.ACTIVE, false, RoleName.ADMIN));
        JwtPrincipal operational = new JwtPrincipal(userId, Set.of("ADMIN"), 0L, false);
        principal = new UsernamePasswordAuthenticationToken(new StompPrincipal(userId.toString(), operational), null, List.of());

        connect("op-session");
        // Operational admin cannot subscribe to /user/queue/admin
        assertThatThrownBy(() -> subscribe("op-session", "sub-op", "/user/queue/admin"))
                .isInstanceOf(AccessDeniedException.class);

        // Primary admin CAN subscribe
        UUID primaryId = UUID.randomUUID();
        User primaryUser = User.reconstitute(primaryId, "primary@example.com", "hash", "Primary", "Admin", null,
                UserStatus.ACTIVE, Set.of(new Role((short) 3, RoleName.ADMIN)), Instant.now(), Instant.now(), true);
        when(users.findById(primaryId)).thenReturn(Optional.of(primaryUser));
        JwtPrincipal primaryPrinc = new JwtPrincipal(primaryId, Set.of("ADMIN"), 0L, true);
        Authentication primaryAuth = new UsernamePasswordAuthenticationToken(new StompPrincipal(primaryId.toString(), primaryPrinc), null, List.of());

        Message<byte[]> connectPrimary = message(SimpMessageType.CONNECT, "primary-session", null, null, primaryAuth);
        broker.handleMessage(connectPrimary);
        registry.onApplicationEvent(new SessionConnectedEvent(this, connectPrimary, primaryAuth));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("primary-session");
        accessor.setSubscriptionId("sub-pri");
        accessor.setDestination("/user/queue/admin");
        accessor.setUser(primaryAuth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        inboundAuthorization.preSend(subMessage, inbound);
        registry.onApplicationEvent(new SessionSubscribeEvent(this, subMessage, primaryAuth));
        userDestinations.handleMessage(subMessage);

        // Publish to primary admin queue
        Message<byte[]> msg = message(SimpMessageType.MESSAGE, null, null, "/user/" + primaryId + "/queue/admin", null);
        userDestinations.handleMessage(msg);

        assertThat(delivered).hasSize(1);
        assertThat(SimpMessageHeaderAccessor.getSessionId(delivered.getFirst().getHeaders()))
                .isEqualTo("primary-session");
    }

    private User userWithPrimary(UserStatus status, boolean primary, RoleName... roleNames) {
        var roles = java.util.Arrays.stream(roleNames).map(role -> new Role((short) 1, role)).collect(java.util.stream.Collectors.toSet());
        return User.reconstitute(userId, "test@example.com", "hash", "Test", "User", null, status, roles, Instant.now(), Instant.now(), primary);
    }

    private String topic() { return "/topic/stores/" + storeId + "/orders"; }

    private User user(UserStatus status, RoleName... roleNames) {
        var roles = java.util.Arrays.stream(roleNames).map(role -> new Role((short) 1, role)).collect(java.util.stream.Collectors.toSet());
        return User.reconstitute(userId, "test@example.com", "hash", "Test", "User", null, status, roles, Instant.now(), Instant.now());
    }

    private void connect(String session) {
        Message<byte[]> connect = message(SimpMessageType.CONNECT, session, null, null, principal);
        broker.handleMessage(connect);
        registry.onApplicationEvent(new SessionConnectedEvent(this, connect, principal));
    }

    private void subscribe(String session, String id, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId(session);
        accessor.setSubscriptionId(id);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        inboundAuthorization.preSend(message, inbound);
        registry.onApplicationEvent(new SessionSubscribeEvent(this, message, principal));
        if (destination.equals("/user/queue/orders") || destination.equals("/user/queue/admin")) {
            userDestinations.handleMessage(message);
        } else {
            broker.handleMessage(message);
        }
    }

    private void publish(String destination) {
        broker.handleMessage(message(SimpMessageType.MESSAGE, null, null, destination, null));
    }

    private void publishCustomer() {
        Message<byte[]> message = message(SimpMessageType.MESSAGE, null, null,
                "/user/" + userId + "/queue/orders", null);
        userDestinations.handleMessage(message);
    }

    private Message<byte[]> message(SimpMessageType type, String session, String id, String destination, Authentication user) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        if (session != null) accessor.setSessionId(session);
        if (id != null) accessor.setSubscriptionId(id);
        if (destination != null) accessor.setDestination(destination);
        if (user != null) accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
