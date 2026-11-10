package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.application.command.LoginCommand;
import com.jaldishop.backend.identity.application.command.RegisterCustomerCommand;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.application.service.AuthenticateUserService;
import com.jaldishop.backend.identity.application.service.ForgotPasswordService;
import com.jaldishop.backend.identity.application.service.ResetPasswordService;
import com.jaldishop.backend.identity.application.command.ForgotPasswordCommand;
import com.jaldishop.backend.identity.application.command.ResetPasswordCommand;
import com.jaldishop.backend.identity.application.service.RegisterCustomerService;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real TCP/STOMP and PostgreSQL; creates only its own customer fixtures, and never sends mail. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.address=127.0.0.1", "app.cors.allowed-origins=http://recovery.test",
        "app.password-reset.mail.mode=disabled",
        "app.password-reset.mail.real-delivery-enabled=false",
        "logging.level.org.springframework.messaging=INFO", "logging.level.org.springframework.web.socket=INFO"})
class PasswordRecoveryWebSocketIntegrationTest {
    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", database::url);
        r.add("spring.datasource.username", database::username);
        r.add("spring.datasource.password", database::password);
        r.add("spring.flyway.url", database::url);
        r.add("spring.flyway.user", database::username);
        r.add("spring.flyway.password", database::password);
    }
    @LocalServerPort int port;
    @Autowired RegisterCustomerService registration;
    @Autowired AuthenticateUserService login;
    @Autowired ForgotPasswordService forgot;
    @Autowired ResetPasswordService resetPassword;
    @Autowired SimpMessagingTemplate messages;
    @Autowired SimpUserRegistry registry;
    @Autowired SimpleBrokerMessageHandler broker;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PasswordResetMail mail;
    private final List<WebSocketStompClient> clients = new ArrayList<>();
    private UUID userId;
    private String email;
    private final BlockingQueue<String> recoveryTokens = new LinkedBlockingQueue<>();
    private String previousJwt;

    @BeforeEach void fixture() {
        TestPostgresConfig.requireDestructiveIntegrationDatabase(jdbc.queryForObject("SELECT current_database()", String.class));
        email = "recovery-ws-" + UUID.randomUUID() + "@example.test";
        userId = registration.execute(new RegisterCustomerCommand(email, "old-password", "Recovery", "Socket", null)).getId();
        previousJwt = login.execute(new LoginCommand(email, "old-password")).token();
        doAnswer(invocation -> { recoveryTokens.offer(invocation.getArgument(1)); return null; })
                .when(mail).send(eq(email), any(), any());
    }
    @AfterEach void cleanup() {
        clients.forEach(WebSocketStompClient::stop);
        if (userId != null) jdbc.update("DELETE FROM users WHERE id=? AND email=?", userId, email);
    }
    @Test void oldJwtCannotConnectAfterPasswordReset() throws Exception {
        assertThat(connect(previousJwt).isConnected()).isTrue();
        resetPassword();
        assertThatThrownBy(() -> connect(previousJwt)).isInstanceOf(ExecutionException.class);
        assertThat(connect(currentJwt()).isConnected()).isTrue();
    }
    @Test void alreadySubscribedSessionStopsReceivingAfterPasswordReset() throws Exception {
        StompSession oldSession = connect(previousJwt);
        BlockingQueue<String> oldMessages = subscribe(oldSession);
        messages.convertAndSendToUser(userId.toString(), "/queue/orders", "before-reset");
        assertThat(oldMessages.poll(5, TimeUnit.SECONDS)).isEqualTo("before-reset");
        resetPassword();
        BlockingQueue<String> currentMessages = subscribe(connect(currentJwt()));
        messages.convertAndSendToUser(userId.toString(), "/queue/orders", "after-reset");
        assertThat(currentMessages.poll(5, TimeUnit.SECONDS)).isEqualTo("after-reset");
        assertThat(oldMessages.poll(2, TimeUnit.SECONDS)).isNull();
    }
    private void resetPassword() throws InterruptedException {
        forgot.execute(new ForgotPasswordCommand(email));
        String token = recoveryTokens.poll(8, TimeUnit.SECONDS);
        if (token == null) throw new AssertionError("Recovery mail was not captured");
        resetPassword.execute(new ResetPasswordCommand(token, "new-password"));
    }
    private String currentJwt() { return login.execute(new LoginCommand(email, "new-password")).token(); }
    private StompSession connect(String jwt) throws Exception {
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());
        clients.add(client);
        var handshake = new WebSocketHttpHeaders();
        handshake.setOrigin("http://recovery.test");
        var connect = new StompHeaders(); connect.add("Authorization", "Bearer " + jwt);
        return client.connectAsync("ws://127.0.0.1:" + port + "/ws", handshake, connect,
                new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);
    }
    private BlockingQueue<String> subscribe(StompSession session) throws Exception {
        var received = new LinkedBlockingQueue<String>();
        var headers = new StompHeaders();
        headers.setDestination("/user/queue/orders");
        headers.setId(UUID.randomUUID().toString());
        var subscription = session.subscribe(headers, new StompFrameHandler() {
            public Type getPayloadType(StompHeaders headers) { return String.class; }
            public void handleFrame(StompHeaders headers, Object payload) { received.offer((String) payload); }
        });
        // The simple broker does not support receipts; wait for its actual subscription registry.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var user = registry.getUser(userId.toString());
            if (user != null) for (var serverSession : user.getSessions()) {
                var header = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                header.setDestination("/queue/orders-user" + serverSession.getId());
                var subscriptions = broker.getSubscriptionRegistry().findSubscriptions(
                        MessageBuilder.createMessage(new byte[0], header.getMessageHeaders()));
                var ids = subscriptions.get(serverSession.getId());
                if (ids != null && ids.contains(subscription.getSubscriptionId())) return received;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        throw new AssertionError("The real WebSocket subscription was not registered in time");
    }
}
