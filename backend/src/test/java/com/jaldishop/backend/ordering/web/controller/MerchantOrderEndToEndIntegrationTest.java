package com.jaldishop.backend.ordering.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
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
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración End-to-End (E2E) reales del módulo REST de gestión de pedidos
 * del comerciante:
 *
 * <p><strong>Flujo Integral:</strong>
 * HTTP REST autenticado (red real) → autorización y aislamiento multitienda → PostgreSQL →
 * transición de estado bajo bloqueo pesimista → commit → evento WebSocket STOMP recibido por el cliente.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.level.org.springframework.messaging=TRACE",
                "logging.level.org.springframework.web.socket=TRACE",
                "spring.jpa.properties.hibernate.generate_statistics=true"
        })
@Import(MerchantOrderEndToEndIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MerchantOrderEndToEndIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    // Tiendas y usuarios sembrados en V4 / V11
    private static final UUID STORE_A_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID MERCHANT_A_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e1");

    private static final UUID STORE_B_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e6");
    private static final UUID MERCHANT_B_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f1");

    private static final UUID CUSTOMER_A_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");
    private static final UUID CUSTOMER_B_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f2");

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<StompSession> activeSessions = new CopyOnWriteArrayList<>();

    private static String merchantAToken;
    private static String merchantBToken;
    private static String customerAToken;
    private static String customerBToken;

    private static UUID sampleOrderAId;
    private static UUID orderBId;

    @BeforeAll
    static void seedDatabase() throws SQLException {
        Instant baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        // 1. Sembrar 25 pedidos en Tienda A para Cliente A con items e historial
        for (int i = 0; i < 25; i++) {
            UUID orderId = UUID.randomUUID();
            String orderNumber = "JAL-E2E-A-" + String.format("%03d", i + 1);
            OrderStatus status = (i < 15) ? OrderStatus.CONFIRMED : (i < 21 ? OrderStatus.IN_PREPARATION : OrderStatus.READY);
            Instant confirmedAt = baseTime.plus(i * 2, ChronoUnit.MINUTES);

            seedOrderInPostgres(orderId, orderNumber, CUSTOMER_A_USER_ID, STORE_A_ID, status, confirmedAt);
            if (i == 0) {
                sampleOrderAId = orderId;
            }
        }

        // 2. Sembrar 1 pedido en Tienda B para Cliente B
        orderBId = UUID.randomUUID();
        seedOrderInPostgres(orderBId, "JAL-E2E-B-001", CUSTOMER_B_USER_ID, STORE_B_ID, OrderStatus.CONFIRMED, baseTime);
    }

    @BeforeEach
    void setupTokens() {
        if (merchantAToken == null) {
            merchantAToken = jwtService.generateToken(MERCHANT_A_USER_ID, Set.of("MERCHANT"));
            merchantBToken = jwtService.generateToken(MERCHANT_B_USER_ID, Set.of("MERCHANT"));
            customerAToken = jwtService.generateToken(CUSTOMER_A_USER_ID, Set.of("CUSTOMER"));
            customerBToken = jwtService.generateToken(CUSTOMER_B_USER_ID, Set.of("CUSTOMER"));
        }
    }

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
    // 1. CONSULTA AUTORIZADA Y PAGINACIÓN HTTP REAL
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("E2E HTTP: Comerciante A consulta pedidos de su tienda de forma paginada")
    void shouldListOrdersForAuthorizedMerchantOverHttp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders?page=0&size=20"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode root = jsonMapper.readTree(response.body());

        assertThat(root.get("page").asInt()).isZero();
        assertThat(root.get("size").asInt()).isEqualTo(20);
        assertThat(root.get("totalElements").asInt()).isGreaterThanOrEqualTo(25);
        assertThat(root.get("content").isArray()).isTrue();
        assertThat(root.get("content").size()).isEqualTo(20);

        // Aislamiento: todos pertenecen a la Tienda A
        for (JsonNode item : root.get("content")) {
            assertThat(item.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("E2E HTTP: Filtro opcional por estado en listado del comerciante")
    void shouldFilterOrdersByStatusOverHttp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders?status=IN_PREPARATION&size=20"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode root = jsonMapper.readTree(response.body());
        assertThat(root.get("content").isArray()).isTrue();

        for (JsonNode item : root.get("content")) {
            assertThat(item.get("status").asText()).isEqualTo("IN_PREPARATION");
            assertThat(item.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("E2E HTTP: Detalle completo de un pedido existente dentro de la tienda")
    void shouldGetOrderDetailOverHttp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + sampleOrderAId))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode detail = jsonMapper.readTree(response.body());

        assertThat(detail.get("id").asText()).isEqualTo(sampleOrderAId.toString());
        assertThat(detail.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        assertThat(detail.get("customerUserId").asText()).isEqualTo(CUSTOMER_A_USER_ID.toString());
        assertThat(detail.get("totalAmount").asDouble()).isEqualTo(50.00);
        assertThat(detail.get("currency").asText()).isEqualTo("PEN");
        assertThat(detail.get("items").isArray()).isTrue();
        assertThat(detail.get("statusHistory").isArray()).isTrue();
    }

    // =========================================================================
    // 2. SEGURIDAD, AUTORIZACIÓN Y AISLAMIENTO MULTITIENDA
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("E2E Seguridad: Comerciante A intentando acceder o mutar pedidos de Tienda B es rechazado (403)")
    void shouldRejectCrossStoreAccess() throws Exception {
        // 1. GET listado de otra tienda
        HttpRequest getListRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_B_ID + "/orders"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> getListResponse = httpClient.send(getListRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(getListResponse.statusCode()).isEqualTo(403);

        // 2. GET detalle de otra tienda
        HttpRequest getDetailRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_B_ID + "/orders/" + orderBId))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> getDetailResponse = httpClient.send(getDetailRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(getDetailResponse.statusCode()).isEqualTo(403);

        // 3. PATCH de otra tienda
        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_B_ID + "/orders/" + orderBId + "/status"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"IN_PREPARATION\"}"))
                .build();
        HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(patchResponse.statusCode()).isEqualTo(403);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("E2E Seguridad: Cliente sin rol MERCHANT intentando acceder es rechazado (403)")
    void shouldRejectCustomerAccessToMerchantEndpoints() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders"))
                .header("Authorization", "Bearer " + customerAToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("E2E Seguridad: Petición sin token JWT es rechazada (401)")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("E2E HTTP: Pedido inexistente o ajeno en la ruta devuelve 404")
    void shouldReturn404ForNonExistentOrCrossStoreOrder() throws Exception {
        // Pedido inexistente
        HttpRequest nonExistentRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + UUID.randomUUID()))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> nonExistentResponse = httpClient.send(nonExistentRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(nonExistentResponse.statusCode()).isEqualTo(404);

        // Pedido de Tienda B consultado a través de Tienda A
        HttpRequest crossStoreRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + orderBId))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> crossStoreResponse = httpClient.send(crossStoreRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(crossStoreResponse.statusCode()).isEqualTo(404);
    }

    // =========================================================================
    // 3. TRANSICIÓN DE ESTADO PATCH + WEBSOCKET STOMP EN TIEMPO REAL
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("E2E Completo: PATCH 200 persiste en PostgreSQL y emite ORDER_STATUS_CHANGED vía WebSocket al cliente correcto")
    void shouldChangeOrderStatusAndEmitWebSocketEventToOwnerCustomer() throws Exception {
        // 1. Conectar cliente A (dueño del pedido) y cliente B (otro cliente ajeno) a WebSocket STOMP
        StompSession sessionA = connectStomp(customerAToken, "http://localhost:" + port);
        StompSession sessionB = connectStomp(customerBToken, "http://localhost:" + port);

        BlockingQueue<JsonNode> queueA = subscribeTo(sessionA, "/user/queue/orders");
        BlockingQueue<JsonNode> queueB = subscribeTo(sessionB, "/user/queue/orders");

        // 2. Sembrar un pedido CONFIRMED dedicado para la prueba de transición
        UUID transitionOrderId = UUID.randomUUID();
        seedOrderInPostgres(transitionOrderId, "JAL-E2E-WS-001", CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED, Instant.now());

        // 3. El comerciante A envía PATCH /status a IN_PREPARATION por HTTP red real
        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + transitionOrderId + "/status"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"IN_PREPARATION\"}"))
                .build();

        HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());

        // 4. Verificar respuesta HTTP 200 OK con detalle actualizado
        assertThat(patchResponse.statusCode()).isEqualTo(200);
        JsonNode responseBody = jsonMapper.readTree(patchResponse.body());
        assertThat(responseBody.get("id").asText()).isEqualTo(transitionOrderId.toString());
        assertThat(responseBody.get("status").asText()).isEqualTo("IN_PREPARATION");

        // 5. Verificar persistencia real en PostgreSQL
        Order persistedOrder = orderRepository.findById(transitionOrderId).orElseThrow();
        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);

        // 6. Verificar entrega real de frame STOMP a Cliente A en /user/queue/orders
        JsonNode stompEvent = queueA.poll(5, TimeUnit.SECONDS);
        assertThat(stompEvent).isNotNull();
        assertThat(stompEvent.get("eventType").asText()).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(stompEvent.get("orderId").asText()).isEqualTo(transitionOrderId.toString());
        assertThat(stompEvent.get("storeId").asText()).isEqualTo(STORE_A_ID.toString());
        assertThat(stompEvent.get("customerId").asText()).isEqualTo(CUSTOMER_A_USER_ID.toString());
        assertThat(stompEvent.get("previousStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(stompEvent.get("newStatus").asText()).isEqualTo("IN_PREPARATION");

        // 7. Aislamiento entre clientes: Cliente B NO debe recibir nada
        JsonNode leakEvent = queueB.poll(1, TimeUnit.SECONDS);
        assertThat(leakEvent).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("E2E Idempotencia: PATCH al mismo estado retorna 200 y NO emite notificación STOMP duplicada")
    void shouldHandleIdempotentPatchWithoutDuplicateStompNotification() throws Exception {
        StompSession sessionA = connectStomp(customerAToken, "http://localhost:" + port);
        BlockingQueue<JsonNode> queueA = subscribeTo(sessionA, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        seedOrderInPostgres(orderId, "JAL-E2E-IDEMP-001", CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.IN_PREPARATION, Instant.now());

        // Enviar PATCH con el mismo estado actual (IN_PREPARATION)
        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + orderId + "/status"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"IN_PREPARATION\"}"))
                .build();

        HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(patchResponse.statusCode()).isEqualTo(200);
        JsonNode responseBody = jsonMapper.readTree(patchResponse.body());
        assertThat(responseBody.get("status").asText()).isEqualTo("IN_PREPARATION");

        // Ningún mensaje debe ser publicado vía STOMP por el no-op de idempotencia
        JsonNode duplicateEvent = queueA.poll(1, TimeUnit.SECONDS);
        assertThat(duplicateEvent).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("E2E Regla de Negocio: Transición inválida (RN-ORD-07) responde 422 y NO emite STOMP")
    void shouldRejectInvalidTransitionWithoutEmittingStomp() throws Exception {
        StompSession sessionA = connectStomp(customerAToken, "http://localhost:" + port);
        BlockingQueue<JsonNode> queueA = subscribeTo(sessionA, "/user/queue/orders");

        UUID orderId = UUID.randomUUID();
        seedOrderInPostgres(orderId, "JAL-E2E-INVALID-001", CUSTOMER_A_USER_ID, STORE_A_ID, OrderStatus.CONFIRMED, Instant.now());

        // Transición inválida directa CONFIRMED -> READY
        HttpRequest patchRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + orderId + "/status"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"READY\"}"))
                .build();

        HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(patchResponse.statusCode()).isEqualTo(422);
        JsonNode errorBody = jsonMapper.readTree(patchResponse.body());
        assertThat(errorBody.get("code").asText()).isEqualTo("RN-ORD-07");

        // Estado en PostgreSQL permanece inalterado
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Sin evento en WebSocket
        JsonNode invalidEvent = queueA.poll(1, TimeUnit.SECONDS);
        assertThat(invalidEvent).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("E2E Errores HTTP: Parámetros inválidos responden 400 Bad Request")
    void shouldReturn400ForInvalidParameters() throws Exception {
        // Page negativo
        HttpRequest negPage = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders?page=-1"))
                .header("Authorization", "Bearer " + merchantAToken)
                .GET().build();
        assertThat(httpClient.send(negPage, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);

        // Size excesivo
        HttpRequest bigSize = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders?size=150"))
                .header("Authorization", "Bearer " + merchantAToken)
                .GET().build();
        assertThat(httpClient.send(bigSize, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);

        // Status body malformado
        HttpRequest badBody = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/merchants/stores/" + STORE_A_ID + "/orders/" + sampleOrderAId + "/status"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"status\":\"ESTADO_INEXISTENTE\"}"))
                .build();
        assertThat(httpClient.send(badBody, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
    }

    // =========================================================================
    // 4. VERIFICACIÓN EMPÍRICA DE CONSULTAS SQL (BATCH SIZE)
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("Rendimiento SQL: Paginación de 20 pedidos ejecuta exactamente 4 consultas SQL mediante @BatchSize")
    void shouldExecuteExactly4SqlQueriesFor20OrdersPage() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();

        // Ejecutar listado de 20 pedidos con ítems e historial cargados
        var page = orderRepository.findByStoreId(STORE_A_ID, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(20);
        assertThat(page.getContent()).allMatch(o -> !o.getItems().isEmpty());
        assertThat(page.getContent()).allMatch(o -> !o.getStatusHistory().isEmpty());

        long queryCount = sessionFactory.getStatistics().getPrepareStatementCount();

        // 1 orders + 1 count + 1 order_items batch + 1 order_status_history batch = 4
        assertThat(queryCount).isEqualTo(4L);
    }

    // =========================================================================
    // UTILITARIOS DE SOPORTE PARA WEBSOCKET Y POSTGRESQL
    // =========================================================================

    private static void seedOrderInPostgres(UUID orderId, String orderNumber, UUID userId, UUID storeId,
                                           OrderStatus status, Instant confirmedAt) throws SQLException {
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID variantId = storeId.equals(STORE_A_ID)
                ? UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf301") // Torta Entera en Store A
                : UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf307"); // Cappuccino en Store B
        createdOrderIds.add(orderId);
        createdPaymentIds.add(paymentId);
        createdCapResIds.add(capResId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '20 days', '10:00:00', '12:00:00', 'COMMITTED', NOW() + INTERVAL '30 minutes', NULL, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.setObject(4, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(5, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 2. Payment
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.setObject(3, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(4, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(5, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 3. Order
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date, service_start_time, service_end_time,
                                        customer_name, customer_phone, customer_email, delivery_address, delivery_reference, currency,
                                        products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', CURRENT_DATE + INTERVAL '20 days', '10:00:00', '12:00:00',
                            'Cliente Test', '987654321', 'test@jaldi.pe', NULL, NULL, 'PEN',
                            50.00, 0.00, 0.00, 7.63, 50.00, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, orderNumber);
                ps.setObject(3, userId);
                ps.setObject(4, storeId);
                ps.setObject(5, paymentId);
                ps.setObject(6, capResId);
                ps.setString(7, status.name());
                ps.setObject(8, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(9, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 4. Order Item
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_items (id, order_id, store_id, variant_id, product_name, variant_name, attributes_snapshot, quantity, unit_price_amount, subtotal_amount)
                    VALUES (gen_random_uuid(), ?, ?, ?, 'Torta Especial', 'Entera', '{"Tamaño":"Entera"}'::jsonb, 1, 50.00, 50.00)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setObject(2, storeId);
                ps.setObject(3, variantId);
                ps.executeUpdate();
            }

            // 5. Order Status History
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (gen_random_uuid(), ?, NULL, ?, 'Semilla E2E', ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, status.name());
                ps.setObject(3, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }
        }
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new AbstractMessageConverter(MimeTypeUtils.APPLICATION_JSON) {
            @Override
            protected boolean supports(Class<?> clazz) {
                return true;
            }

            @Override
            protected Object convertFromInternal(org.springframework.messaging.Message<?> message, Class<?> targetClass, Object conversionHint) {
                Object payload = message.getPayload();
                if (payload instanceof byte[] bytes) {
                    try {
                        return jsonMapper.readTree(bytes);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return payload;
            }

            @Override
            protected Object convertToInternal(Object payload, org.springframework.messaging.MessageHeaders headers, Object conversionHint) {
                if (payload instanceof byte[] bytes) return bytes;
                try {
                    return jsonMapper.writeValueAsBytes(payload);
                } catch (Exception e) {
                    return payload.toString().getBytes(StandardCharsets.UTF_8);
                }
            }
        });
        return client;
    }

    private StompSession connectStomp(String token, String origin) throws Exception {
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
                new StompSessionHandlerAdapter() {}
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
                    } catch (Exception ignored) {
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
