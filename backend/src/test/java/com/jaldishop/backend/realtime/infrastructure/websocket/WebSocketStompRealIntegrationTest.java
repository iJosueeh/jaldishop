package com.jaldishop.backend.realtime.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.SecretKey;
import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración E2E de WebSocket + STOMP con servidor embebido real,
 * PostgreSQL real, JWT real y clientes STOMP reales conectados vía WebSocket.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "logging.level.org.springframework.messaging=TRACE",
                "logging.level.org.springframework.web.socket=TRACE"
        })
@Import(WebSocketStompRealIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketStompRealIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    // IDs de tiendas y usuarios sembrados en PostgreSQL (migración V4)
    private static final UUID STORE_A_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5"); // Pastelería Dulce Deleite
    private static final UUID MERCHANT_A_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e1");

    private static final UUID STORE_B_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e6"); // Café & Grano
    private static final UUID MERCHANT_B_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f1");

    private static final UUID CUSTOMER_A_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2"); // cliente.demo
    private static final UUID CUSTOMER_B_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f2"); // cliente.juan

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCartIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
    }

    @LocalServerPort
    private int port;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ConfirmPurchaseService confirmPurchaseService;

    @Autowired
    private ChangeOrderStatusService changeOrderStatusService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher publisher;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final List<StompSession> activeSessions = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDownSessions() {
        for (StompSession session : activeSessions) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
        activeSessions.clear();
    }

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            for (UUID cartId : createdCartIds) {
                try (var ps = conn.prepareStatement("DELETE FROM cart_items WHERE cart_id = ?")) {
                    ps.setObject(1, cartId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM carts WHERE id = ?")) {
                    ps.setObject(1, cartId);
                    ps.executeUpdate();
                }
            }

            for (UUID orderId : createdOrderIds) {
                try (var ps = conn.prepareStatement("DELETE FROM order_status_history WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
            }

            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE capacity_reservation_id = ?)")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE capacity_reservation_id = ?)")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM orders WHERE capacity_reservation_id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }

            for (UUID paymentId : createdPaymentIds) {
                try (var ps = conn.prepareStatement("DELETE FROM payments WHERE id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
            }

            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM capacity_reservations WHERE id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }
        }
    }

    // =========================================================================
    // 2. AUTENTICACIÓN Y SEGURIDAD
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("Seguridad: CONNECT con JWT válido establece conexión STOMP exitosamente")
    void shouldConnectSuccessfullyWithValidJwt() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        StompSession session = connectStomp(token, "http://localhost:" + port);

        assertThat(session.isConnected()).isTrue();
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("Seguridad: CONNECT con JWT inválido/corrupto es rechazado y no establece sesión")
    void shouldRejectConnectWithInvalidJwt() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidpayload.invalidsignature";
        assertThatThrownBy(() -> connectStomp(invalidToken, "http://localhost:" + port))
                .isInstanceOf(Exception.class);
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("Seguridad: CONNECT con JWT expirado es rechazado y no establece sesión")
    void shouldRejectConnectWithExpiredJwt() {
        String expiredToken = createExpiredToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        assertThatThrownBy(() -> connectStomp(expiredToken, "http://localhost:" + port))
                .isInstanceOf(Exception.class);
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    @DisplayName("Seguridad: CONNECT sin JWT (token ausente) es rechazado")
    void shouldRejectConnectWithoutJwt() {
        assertThatThrownBy(() -> connectStomp(null, "http://localhost:" + port))
                .isInstanceOf(Exception.class);
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    @DisplayName("Seguridad: Origen permitido (localhost) permite handshake; origen no autorizado es rechazado")
    void shouldAllowPermittedOriginAndRejectUnauthorizedOrigin() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));

        // 1. Origen permitido
        StompSession permittedSession = connectStomp(token, "http://localhost:" + port);
        assertThat(permittedSession.isConnected()).isTrue();

        // 2. Origen no autorizado
        assertThatThrownBy(() -> connectStomp(token, "https://sitio-malicioso.com"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    @DisplayName("Seguridad: Intentos de SEND de negocio por WebSocket son bloqueados incondicionalmente")
    void shouldBlockClientSendCommand() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        TestSessionHandler handler = new TestSessionHandler();
        StompSession session = connectStompWithHandler(token, "http://localhost:" + port, handler);

        // El cliente STOMP intenta enviar un comando por WebSocket
        session.send("/topic/stores/" + STORE_A_ID + "/orders", "payload-malicioso".getBytes(StandardCharsets.UTF_8));

        // El servidor cierra la conexión y reporta error de transporte / AccessDeniedException
        Throwable error = handler.errors.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Order(16)
    @DisplayName("Seguridad: Intentos de SUBSCRIBE a destinos no autorizados son rechazados")
    void shouldRejectSubscriptionToUnauthorizedDestinations() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        TestSessionHandler handler = new TestSessionHandler();
        StompSession session = connectStompWithHandler(token, "http://localhost:" + port, handler);

        // Suscripción a un destino no mapeado
        session.subscribe("/topic/admin/secret-channel", new NoOpFrameHandler());

        Throwable error = handler.errors.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
    }

    // =========================================================================
    // 3. AISLAMIENTO MULTITIENDA Y PRIVACIDAD
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(20)
    @DisplayName("Multitienda: Comerciante A recibe eventos de su Tienda A; Comerciante B no los recibe")
    void shouldIsolateEventsBetweenStores() throws Exception {
        String tokenMerchantA = jwtService.generateToken(MERCHANT_A_USER_ID, Set.of("MERCHANT"));
        String tokenMerchantB = jwtService.generateToken(MERCHANT_B_USER_ID, Set.of("MERCHANT"));

        StompSession sessionMerchantA = connectStomp(tokenMerchantA, "http://localhost:" + port);
        StompSession sessionMerchantB = connectStomp(tokenMerchantB, "http://localhost:" + port);

        BlockingQueue<JsonNode> queueA = subscribeTo(sessionMerchantA, "/topic/stores/" + STORE_A_ID + "/orders");
        BlockingQueue<JsonNode> queueB = subscribeTo(sessionMerchantB, "/topic/stores/" + STORE_B_ID + "/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-ISO-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        // Despachamos evento para Tienda A
        publisher.publishOrderConfirmed(new OrderConfirmedDomainEvent(
                orderId, orderNumber, STORE_A_ID, new BigDecimal("75.00"), "PEN"
        ));

        JsonNode receivedByA = queueA.poll(5, TimeUnit.SECONDS);
        assertThat(receivedByA).isNotNull();
        assertThat(receivedByA.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        assertThat(receivedByA.get("orderNumber").asText()).isEqualTo(orderNumber);
        assertThat(receivedByA.get("eventType").asText()).isEqualTo("ORDER_CONFIRMED");

        // Merchant B en Tienda B NO debe recibir nada de la Tienda A
        JsonNode receivedByB = queueB.poll(2, TimeUnit.SECONDS);
        assertThat(receivedByB).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(21)
    @DisplayName("Multitienda: Comerciante A es rechazado al intentar suscribirse a la Tienda B ajena")
    void shouldRejectMerchantSubscribingToAnotherStore() throws Exception {
        String tokenMerchantA = jwtService.generateToken(MERCHANT_A_USER_ID, Set.of("MERCHANT"));
        TestSessionHandler handler = new TestSessionHandler();
        StompSession session = connectStompWithHandler(tokenMerchantA, "http://localhost:" + port, handler);

        // Merchant A intenta suscribirse a la Tienda B ajena
        session.subscribe("/topic/stores/" + STORE_B_ID + "/orders", new NoOpFrameHandler());

        Throwable error = handler.errors.poll(3, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Order(22)
    @DisplayName("Privacidad: Cliente A recibe eventos en /user/queue/orders; Cliente B no los recibe")
    void shouldIsolateEventsBetweenCustomers() throws Exception {
        String tokenCustomerA = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        String tokenCustomerB = jwtService.generateToken(CUSTOMER_B_USER_ID, Set.of("CUSTOMER"));

        StompSession sessionCustomerA = connectStomp(tokenCustomerA, "http://localhost:" + port);
        StompSession sessionCustomerB = connectStomp(tokenCustomerB, "http://localhost:" + port);

        BlockingQueue<JsonNode> queueA = subscribeTo(sessionCustomerA, "/user/queue/orders");
        BlockingQueue<JsonNode> queueB = subscribeTo(sessionCustomerB, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-PRIV-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        // Cliente A recibe el evento
        JsonNode eventA = queueA.poll(5, TimeUnit.SECONDS);
        assertThat(eventA).isNotNull();
        assertThat(eventA.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(eventA.get("customerId").asText()).isEqualTo(CUSTOMER_A_USER_ID.toString());
        assertThat(eventA.get("newStatus").asText()).isEqualTo("IN_PREPARATION");

        // Cliente B no recibe absolutamente nada
        JsonNode eventB = queueB.poll(2, TimeUnit.SECONDS);
        assertThat(eventB).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(23)
    @DisplayName("Privacidad: Mismo cliente conectado desde dos sesiones recibe las notificaciones en ambas")
    void shouldDeliverToMultipleSessionsOfSameCustomer() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));

        StompSession session1 = connectStomp(token, "http://localhost:" + port);
        StompSession session2 = connectStomp(token, "http://localhost:" + port);

        BlockingQueue<JsonNode> queue1 = subscribeTo(session1, "/user/queue/orders");
        BlockingQueue<JsonNode> queue2 = subscribeTo(session2, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-MULTI-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        JsonNode eventSession1 = queue1.poll(5, TimeUnit.SECONDS);
        JsonNode eventSession2 = queue2.poll(5, TimeUnit.SECONDS);

        assertThat(eventSession1).isNotNull();
        assertThat(eventSession2).isNotNull();
        assertThat(eventSession1.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(eventSession2.get("orderId").asText()).isEqualTo(orderId.toString());
    }

    // =========================================================================
    // 4. EVENTOS DE NEGOCIO REALES (CONFIRM PURCHASE, CHANGE ORDER STATUS, ETC)
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(30)
    @DisplayName("Negocio Real: ConfirmPurchaseService emite frame STOMP MESSAGE real a /topic/stores/{storeId}/orders")
    void shouldDeliverRealStompFrameOnConfirmPurchase() throws Exception {
        String tokenMerchantA = jwtService.generateToken(MERCHANT_A_USER_ID, Set.of("MERCHANT"));
        StompSession sessionMerchantA = connectStomp(tokenMerchantA, "http://localhost:" + port);

        BlockingQueue<JsonNode> queue = subscribeTo(sessionMerchantA, "/topic/stores/" + STORE_A_ID + "/orders");

        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        seedCartAndProtectedPayment(cartId, capResId, paymentId, CUSTOMER_A_USER_ID, STORE_A_ID);

        ConfirmPurchaseCommand command = new ConfirmPurchaseCommand(
                paymentId,
                CheckoutFulfillmentType.PICKUP,
                "Cliente Real Demo",
                "987654321",
                "cliente.demo@jaldishop.com",
                null,
                null,
                null,
                null
        );

        var confirmation = confirmPurchaseService.execute(command);
        createdOrderIds.add(confirmation.orderId());

        // Validar frame STOMP MESSAGE real recibido por el cliente WebSocket del comerciante
        JsonNode receivedEvent = queue.poll(5, TimeUnit.SECONDS);
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        assertThat(receivedEvent.get("eventType").asText()).isEqualTo("ORDER_CONFIRMED");
        assertThat(receivedEvent.get("totalAmount").asDouble()).isGreaterThan(0.0);
        assertThat(receivedEvent.get("currency").asText()).isEqualTo("PEN");
    }

    @Test
    @org.junit.jupiter.api.Order(31)
    @DisplayName("Negocio Real: ChangeOrderStatusService emite frame STOMP MESSAGE real a /user/queue/orders")
    void shouldDeliverRealStompFrameOnChangeOrderStatus() throws Exception {
        String tokenCustomer = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        StompSession session = connectStomp(tokenCustomer, "http://localhost:" + port);

        BlockingQueue<JsonNode> queue = subscribeTo(session, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-FRAME-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        JsonNode event = queue.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(event.get("orderNumber").asText()).isEqualTo(orderNumber);
        assertThat(event.get("previousStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(event.get("newStatus").asText()).isEqualTo("IN_PREPARATION");
        assertThat(event.get("customerId").asText()).isEqualTo(CUSTOMER_A_USER_ID.toString());
    }

    @Test
    @org.junit.jupiter.api.Order(32)
    @DisplayName("Negocio Real: Rollback transaccional NO emite frame STOMP")
    void shouldNotDeliverFrameWhenTransactionRollsBack() throws Exception {
        String tokenCustomer = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        StompSession session = connectStomp(tokenCustomer, "http://localhost:" + port);

        BlockingQueue<JsonNode> queue = subscribeTo(session, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-ROLL-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() ->
                template.execute(status -> {
                    changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);
                    throw new RuntimeException("Forzando rollback deliberado");
                })
        ).isInstanceOf(RuntimeException.class);

        JsonNode event = queue.poll(2, TimeUnit.SECONDS);
        assertThat(event).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(33)
    @DisplayName("Negocio Real: Reintento idempotente NO emite frame STOMP duplicado")
    void shouldNotDeliverDuplicateFrameOnIdempotentRetry() throws Exception {
        String tokenCustomer = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
        StompSession session = connectStomp(tokenCustomer, "http://localhost:" + port);

        BlockingQueue<JsonNode> queue = subscribeTo(session, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-NOOP-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        // Primera llamada: transición efectiva
        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);
        JsonNode firstEvent = queue.poll(5, TimeUnit.SECONDS);
        assertThat(firstEvent).isNotNull();

        // Segunda llamada idéntica (reintento HTTP): no-op
        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        // No debe llegar ningún frame adicional
        JsonNode secondEvent = queue.poll(2, TimeUnit.SECONDS);
        assertThat(secondEvent).isNull();
    }

    // =========================================================================
    // 5. RECONEXIÓN Y EVENTOS EFÍMEROS
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(40)
    @DisplayName("Reconexión: Desconexión y reconexión con JWT requiere nueva autenticación y re-suscripción")
    void shouldRequireReAuthenticationAndReSubscriptionUponReconnect() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));

        // 1. Conexión inicial
        StompSession session1 = connectStomp(token, "http://localhost:" + port);
        BlockingQueue<JsonNode> queue1 = subscribeTo(session1, "/user/queue/orders");
        assertThat(session1.isConnected()).isTrue();

        // 2. Desconexión explícita
        session1.disconnect();
        assertThat(session1.isConnected()).isFalse();

        // 3. Reconexión: debe abrir nueva sesión STOMP con CONNECT autenticado
        StompSession session2 = connectStomp(token, "http://localhost:" + port);
        assertThat(session2.isConnected()).isTrue();

        // 4. Re-suscripción activa
        BlockingQueue<JsonNode> queue2 = subscribeTo(session2, "/user/queue/orders");

        // 5. Los nuevos eventos se reciben en la sesión re-suscrita
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-RECON-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        JsonNode event = queue2.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.get("orderId").asText()).isEqualTo(orderId.toString());
    }

    @Test
    @org.junit.jupiter.api.Order(41)
    @DisplayName("Reconexión: Los eventos emitidos durante una desconexión son efímeros y no se reciben al reconectar")
    void shouldDemonstrateEphemeralBehaviorDuringDisconnection() throws Exception {
        String token = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));

        // Sesión 1 activa y luego desconectada
        StompSession session1 = connectStomp(token, "http://localhost:" + port);
        session1.disconnect();

        // Mientras el cliente está desconectado, ocurre un evento en el servidor
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-OFFLINE-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED);

        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);

        // El cliente se reconecta con una nueva sesión
        StompSession session2 = connectStomp(token, "http://localhost:" + port);
        BlockingQueue<JsonNode> queue2 = subscribeTo(session2, "/user/queue/orders");

        // El evento ocurrido offline NO se recibe (los mensajes en SimpleBroker son efímeros)
        JsonNode missedEvent = queue2.poll(2, TimeUnit.SECONDS);
        assertThat(missedEvent).isNull();

        // Comprobación de recuperación REST: el estado sí existe en la base de datos PostgreSQL
        Order orderFromDb = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderFromDb.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    // =========================================================================
    // MÉTODOS DE SOPORTE Y DATOS
    // =========================================================================

    private WebSocketStompClient createStompClient() {
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new TestJsonMessageConverter());
        return stompClient;
    }

    static class TestJsonMessageConverter extends AbstractMessageConverter {
        private final ObjectMapper mapper = new ObjectMapper();

        TestJsonMessageConverter() {
            super(MimeTypeUtils.ALL);
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return true;
        }

        @Override
        protected Object convertFromInternal(org.springframework.messaging.Message<?> message, Class<?> targetClass, Object conversionHint) {
            Object payload = message.getPayload();
            byte[] bytes = payload instanceof byte[] b ? b : payload.toString().getBytes(StandardCharsets.UTF_8);
            if (byte[].class.isAssignableFrom(targetClass)) {
                return bytes;
            }
            if (String.class.isAssignableFrom(targetClass)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            try {
                return mapper.readTree(bytes);
            } catch (Exception e) {
                throw new MessageConversionException("Failed to parse JsonNode", e);
            }
        }

        @Override
        protected Object convertToInternal(Object payload, org.springframework.messaging.MessageHeaders headers, Object conversionHint) {
            if (payload instanceof byte[] bytes) {
                return bytes;
            }
            if (payload instanceof String str) {
                return str.getBytes(StandardCharsets.UTF_8);
            }
            try {
                return mapper.writeValueAsBytes(payload);
            } catch (Exception e) {
                return payload.toString().getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    private StompSession connectStomp(String token, String origin) throws Exception {
        return connectStompWithHandler(token, origin, new StompSessionHandlerAdapter() {});
    }

    private StompSession connectStompWithHandler(String token, String origin, StompSessionHandler handler) throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        if (origin != null) {
            handshakeHeaders.add("Origin", origin);
        }

        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }

        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                handshakeHeaders,
                connectHeaders,
                handler
        ).get(5, TimeUnit.SECONDS);

        activeSessions.add(session);
        return session;
    }

    private BlockingQueue<JsonNode> subscribeTo(StompSession session, String destination) {
        BlockingQueue<JsonNode> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof JsonNode node) {
                    queue.offer(node);
                } else if (payload instanceof byte[] bytes) {
                    try {
                        queue.offer(jsonMapper.readTree(bytes));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (payload instanceof String str) {
                    try {
                        queue.offer(jsonMapper.readTree(str));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {
        }
        return queue;
    }

    private String createExpiredToken(UUID userId, Set<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(new Date(System.currentTimeMillis() - 60000))
                .expiration(new Date(System.currentTimeMillis() - 30000))
                .signWith(key)
                .compact();
    }

    private void seedOrderInPostgres(UUID orderId, String orderNumber, UUID userId, UUID storeId, OrderStatus status) {
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        createdOrderIds.add(orderId);
        createdPaymentIds.add(paymentId);
        createdCapResIds.add(capResId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00', 'COMMITTED', NOW() + INTERVAL '30 minutes', NULL, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.executeUpdate();
            }

            // 2. Payment
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', NOW(), NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.executeUpdate();
            }

            // 3. Order
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date, service_start_time, service_end_time,
                                        customer_name, customer_phone, customer_email, delivery_address, delivery_reference, currency,
                                        products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00',
                            'Cliente Test', '987654321', 'test@jaldi.pe', NULL, NULL, 'PEN',
                            50.00, 0.00, 0.00, 7.63, 50.00, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, orderNumber);
                ps.setObject(3, userId);
                ps.setObject(4, storeId);
                ps.setObject(5, paymentId);
                ps.setObject(6, capResId);
                ps.setString(7, status.name());
                ps.executeUpdate();
            }

            // 4. Status history
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (?, ?, NULL, ?, 'Semilla de prueba', NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orderId);
                ps.setString(3, status.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al sembrar pedido en PostgreSQL: " + e.getMessage(), e);
        }
    }

    private void seedCartAndProtectedPayment(UUID cartId, UUID capResId, UUID paymentId, UUID userId, UUID storeId) {
        createdCartIds.add(cartId);
        createdCapResIds.add(capResId);
        createdPaymentIds.add(paymentId);
        UUID variantId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf301"); // Torta Entera

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Limpiar carrito previo si existe
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM carts WHERE user_id = ? AND store_id = ?")) {
                ps.setObject(1, userId);
                ps.setObject(2, storeId);
                ps.executeUpdate();
            }

            // 2. Insert cart
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO carts (id, user_id, store_id, created_at, updated_at)
                    VALUES (?, ?, ?, NOW(), NOW());
                    """)) {
                ps.setObject(1, cartId);
                ps.setObject(2, userId);
                ps.setObject(3, storeId);
                ps.executeUpdate();
            }

            // 3. Insert cart item (con store_id requerido)
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO cart_items (cart_id, variant_id, store_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at)
                    VALUES (?, ?, ?, 1, 50.00, 'PEN', NOW(), NOW());
                    """)) {
                ps.setObject(1, cartId);
                ps.setObject(2, variantId);
                ps.setObject(3, storeId);
                ps.executeUpdate();
            }

            // 4. Insert PAYMENT_PROTECTED capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00', 'PAYMENT_PROTECTED', NOW() + INTERVAL '30 minutes', NOW() + INTERVAL '30 minutes', NOW(), NOW());
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.executeUpdate();
            }

            // 5. Insert approved payment
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', NOW(), NOW(), NOW());
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al preparar carrito y pago en PostgreSQL: " + e.getMessage(), e);
        }
    }

    static class TestSessionHandler extends StompSessionHandlerAdapter {
        final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            errors.offer(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            errors.offer(exception);
        }
    }

    static class NoOpFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Object.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestDatabase {

        @Bean
        DataSource dataSource() {
            TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(config.url());
            dataSource.setUsername(config.username());
            dataSource.setPassword(config.password());
            return dataSource;
        }
    }

}
