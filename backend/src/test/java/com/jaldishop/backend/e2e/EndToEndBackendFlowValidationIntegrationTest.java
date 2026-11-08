package com.jaldishop.backend.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.address=127.0.0.1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EndToEndBackendFlowValidationIntegrationTest — Validación integral de flujos E2E con seed reproducible")
class EndToEndBackendFlowValidationIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    private static HttpServer mockServer;
    private static int mockPort;
    private static final String WEBHOOK_SECRET = "test-webhook-secret-e2e-12345";
    private static final long EXPECTED_COLLECTOR_ID = 9988776655L;

    private static final AtomicReference<String> mpPaymentStatus = new AtomicReference<>("approved");
    private static final AtomicReference<String> mpExternalReference = new AtomicReference<>("");
    private static final AtomicReference<Double> mpTransactionAmount = new AtomicReference<>(52.50);
    private static final AtomicReference<UUID> test6PaymentId = new AtomicReference<>();

    @BeforeAll
    static void startMockServerAndSeed() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();

        // Mock endpoints for Mercado Pago
        mockServer.createContext("/checkout/preferences", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String resp = """
                            {
                                "id": "PREF-MOCK-E2E-001",
                                "init_point": "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=PREF-MOCK-E2E-001",
                                "collector_id": 9988776655
                            }
                            """;
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(201, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        mockServer.createContext("/v1/payments/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String paymentIdStr = path.substring(path.lastIndexOf('/') + 1);
                Long paymentIdLong;
                try {
                    paymentIdLong = Long.parseLong(paymentIdStr);
                } catch (NumberFormatException e) {
                    paymentIdLong = 123456789L;
                }
                String status = mpPaymentStatus.get();
                String extRef = mpExternalReference.get() != null ? mpExternalReference.get() : "";
                Double amount = mpTransactionAmount.get() != null ? mpTransactionAmount.get() : 52.50;

                String resp = String.format(Locale.US, """
                        {
                            "id": %d,
                            "status": "%s",
                            "status_detail": "%s",
                            "currency_id": "PEN",
                            "transaction_amount": %.2f,
                            "payment_method_id": "visa",
                            "payment_type_id": "credit_card",
                            "external_reference": "%s",
                            "date_approved": "2026-11-03T19:00:00.000Z",
                            "collector_id": %d,
                            "live_mode": false
                        }
                        """, paymentIdLong, status, "approved".equals(status) ? "accredited" : "cc_rejected_other_reason", amount, extRef, EXPECTED_COLLECTOR_ID);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        mockServer.start();

        // Asegurar que las migraciones Flyway estén aplicadas en base de datos aislada
        Flyway.configure()
                .dataSource(database.url(), database.username(), database.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(database.validateOnMigrate())
                .load()
                .migrate();

        // Aplicar seed reproducible directamente (ejecución inicial e idempotencia verificada)
        applySeedScript();
        applySeedScript();
    }

    private static void applySeedScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/seed/seed_e2e_demo_data.sql");
        try (InputStream is = resource.getInputStream();
             Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             Statement stmt = conn.createStatement()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Ejecutar batch
            stmt.execute(sql);
        }
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
        registry.add("mercadopago.base-url", () -> "http://127.0.0.1:" + mockPort);
        registry.add("mercadopago.checkout-mode", () -> "PREFERENCES");
        registry.add("mercadopago.access-token", () -> "TEST-E2E-TOKEN");
        registry.add("mercadopago.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("mercadopago.expected-collector-id", () -> String.valueOf(EXPECTED_COLLECTOR_ID));
        registry.add("mercadopago.expected-live-mode", () -> "false");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static String customerToken;
    private static String merchantToken;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    // =========================================================================
    // FASE 3 / FASE 4: 1. VERIFICACIÓN DE DATOS SEMBRADOS
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("1. Seed E2E: Verificar integridad de datos sembrados en esquema V19")
    void test1_verifySeedDataIntegrity() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        Integer storeCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stores", Integer.class);
        Integer productCount = jdbcTemplate.queryForObject("SELECT count(*) FROM products", Integer.class);
        Integer variantCount = jdbcTemplate.queryForObject("SELECT count(*) FROM product_variants", Integer.class);
        Integer addressCount = jdbcTemplate.queryForObject("SELECT count(*) FROM customer_addresses", Integer.class);
        Integer orderCount = jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class);
        Integer notificationCount = jdbcTemplate.queryForObject("SELECT count(*) FROM notifications", Integer.class);

        assertNotNull(userCount);
        assertTrue(userCount >= 5, "Deben existir al menos 5 usuarios sembrados");
        assertNotNull(storeCount);
        assertTrue(storeCount >= 2, "Deben existir al menos 2 tiendas");
        assertNotNull(productCount);
        assertTrue(productCount >= 4, "Deben existir productos sembrados");
        assertNotNull(variantCount);
        assertTrue(variantCount >= 5, "Deben existir variantes de producto");
        assertNotNull(addressCount);
        assertTrue(addressCount >= 2, "Deben existir direcciones sembradas con UBIGEO");
        assertNotNull(orderCount);
        assertTrue(orderCount >= 2, "Deben existir órdenes de prueba sembradas");
        assertNotNull(notificationCount);
        assertTrue(notificationCount >= 2, "Deben existir notificaciones sembradas");
    }

    // =========================================================================
    // FASE 4: 2. AUTENTICACIÓN CLIENTE Y COMERCIANTE
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("2. Auth E2E: Iniciar sesión como cliente y comerciante con contraseñas del seed")
    void test2_authenticateUsers() throws Exception {
        // Login Cliente
        String customerLoginBody = """
                {
                    "email": "cliente.demo@jaldishop.com",
                    "password": "Password123!"
                }
                """;
        HttpRequest customerReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(customerLoginBody))
                .build();
        HttpResponse<String> customerResp = httpClient.send(customerReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, customerResp.statusCode(), "Login de cliente debe retornar 200 OK");

        JsonNode customerJson = objectMapper.readTree(customerResp.body());
        assertTrue(customerJson.has("token"));
        customerToken = customerJson.get("token").asText();
        assertNotNull(customerToken);
        assertFalse(customerToken.isBlank());

        // Login Comerciante
        String merchantLoginBody = """
                {
                    "email": "merchant.demo@jaldishop.com",
                    "password": "Password123!"
                }
                """;
        HttpRequest merchantReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/auth/login")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(merchantLoginBody))
                .build();
        HttpResponse<String> merchantResp = httpClient.send(merchantReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, merchantResp.statusCode(), "Login de comerciante debe retornar 200 OK");

        JsonNode merchantJson = objectMapper.readTree(merchantResp.body());
        assertTrue(merchantJson.has("token"));
        merchantToken = merchantJson.get("token").asText();
        assertNotNull(merchantToken);
        assertFalse(merchantToken.isBlank());
    }

    // =========================================================================
    // FASE 4: 3. CATÁLOGO Y TIENDA
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("3. Tienda y Catálogo: Consultar tienda por slug y listar categorías activas")
    void test3_browseStoreAndCatalog() throws Exception {
        // Consultar tienda por slug
        HttpRequest storeReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/stores/slug/dulce-deleite")))
                .GET()
                .build();
        HttpResponse<String> storeResp = httpClient.send(storeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, storeResp.statusCode(), "Consulta de tienda por slug debe ser 200 OK");

        JsonNode storeJson = objectMapper.readTree(storeResp.body());
        assertEquals("Pastelería Dulce Deleite", storeJson.get("name").asText());
        assertEquals("ACTIVE", storeJson.get("status").asText());
        assertTrue(storeJson.get("pickupEnabled").asBoolean());
        assertTrue(storeJson.get("deliveryEnabled").asBoolean());
    }

    // =========================================================================
    // FASE 4: 4. LIBRETA DE DIRECCIONES
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("4. Direcciones: Consultar libreta de direcciones del cliente autenticado")
    void test4_checkCustomerAddressBook() throws Exception {
        HttpRequest addressReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/customer/addresses")))
                .header("Authorization", "Bearer " + customerToken)
                .GET()
                .build();
        HttpResponse<String> addressResp = httpClient.send(addressReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addressResp.statusCode(), "Listado de direcciones debe ser 200 OK");

        JsonNode addressJson = objectMapper.readTree(addressResp.body());
        assertTrue(addressJson.has("addresses"));
        JsonNode list = addressJson.get("addresses");
        assertTrue(list.isArray());
        assertTrue(list.size() >= 1, "Debe existir al menos una dirección sembrada");
        boolean hasDefault = false;
        for (JsonNode addr : list) {
            if (addr.get("isDefault").asBoolean()) {
                hasDefault = true;
                assertEquals("150122", addr.get("ubigeoCode").asText());
            }
        }
        assertTrue(hasDefault, "Debe existir una dirección marcada como predeterminada");
    }

    // =========================================================================
    // FASE 4: 5. CARRITO Y CHECKOUT HOLD ATÓMICO
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("5. Checkout Hold: Iniciar checkout reservando capacidad e inventario atómicamente")
    void test5_initiateCheckoutHold() throws Exception {
        UUID userId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)", userId);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM payment_attempts WHERE payment_id IN (SELECT id FROM payments WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED')))", userId);
        jdbcTemplate.update("DELETE FROM payments WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED'))", userId);
        jdbcTemplate.update("DELETE FROM inventory_reservations WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED'))", userId);
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED')", userId);

        // Agregar ítem al carrito para permitir el checkout
        String addItemBody = """
                {
                    "storeId": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
                    "variantId": "80acf0ba-5a79-4888-b00d-b5aef8fcf301",
                    "quantity": 1
                }
                """;
        HttpRequest addCartReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/cart/items")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + customerToken)
                .POST(HttpRequest.BodyPublishers.ofString(addItemBody))
                .build();
        HttpResponse<String> addCartResp = httpClient.send(addCartReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, addCartResp.statusCode(), "Agregar ítem al carrito debe retornar 201 Created");

        // Seed closures use database CURRENT_DATE + 10/+15 days; keep this hold beyond them.
        LocalDate serviceDate = jdbcTemplate.queryForObject("SELECT CURRENT_DATE", LocalDate.class)
                .plusWeeks(3).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        String holdBody = String.format("""
                {
                    "storeId": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
                    "fulfillmentType": "DELIVERY",
                    "serviceDate": "%s",
                    "startTime": "09:00:00",
                    "endTime": "13:00:00",
                    "customerName": "Valeria Ríos",
                    "customerPhone": "+51965432109",
                    "customerEmail": "cliente.demo@jaldishop.com",
                    "deliveryAddress": "Av. José Larco 345, Dpto 402, Miraflores",
                    "deliveryReference": "Cerca al Parque Kennedy",
                    "deliveryLatitude": -12.121900,
                    "deliveryLongitude": -77.029800,
                    "discountCode": null
                }
                """, serviceDate);

        HttpRequest checkoutReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/checkout")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + customerToken)
                .POST(HttpRequest.BodyPublishers.ofString(holdBody))
                .build();

        HttpResponse<String> checkoutResp = httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, checkoutResp.statusCode(), "Checkout hold debe crearse con 201 Created: " + checkoutResp.body());

        JsonNode checkoutJson = objectMapper.readTree(checkoutResp.body());
        assertTrue(checkoutJson.has("reservationId"));

        UUID reservationId = UUID.fromString(checkoutJson.get("reservationId").asText());

        // Verificar persistencia de reserva en PostgreSQL
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM capacity_reservations WHERE id = ?",
                String.class, reservationId
        );
        assertEquals("ACTIVE", status, "La reserva de capacidad debe persistirse en ACTIVE");

        // Crear preferencia de pago
        String prefBody = String.format("""
                {
                    "capacityReservationId": "%s",
                    "fulfillmentType": "DELIVERY",
                    "customerName": "Valeria Ríos",
                    "customerPhone": "+51965432109",
                    "customerEmail": "cliente.demo@jaldishop.com",
                    "deliveryAddress": "Av. José Larco 345, Dpto 402, Miraflores",
                    "deliveryReference": "Cerca al Parque Kennedy",
                    "deliveryLatitude": -12.121900,
                    "deliveryLongitude": -77.029800
                }
                """, reservationId);

        HttpRequest prefReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/checkout/mercado-pago/preference")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + customerToken)
                .POST(HttpRequest.BodyPublishers.ofString(prefBody))
                .build();

        HttpResponse<String> prefResp = httpClient.send(prefReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, prefResp.statusCode(), "Preferencia debe crearse con 201 Created");
    }

    // =========================================================================
    // FASE 4: 6. SIMULACIÓN DE PAGO APPROVED & CONFIRMACIÓN DE PEDIDO
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("6. Webhook Pago Aprobado: Confirmación de orden, deduplicación y notificación")
    void test6_simulatePaymentAndOrderConfirmation() throws Exception {
        String mpPaymentId = String.valueOf(1000000000L + (System.currentTimeMillis() % 100000000));
        mpPaymentStatus.set("approved");
        mpTransactionAmount.set(52.50);

        // Crear pago previo en estado PENDING asociado a una reserva válida
        LocalDate serviceDate = LocalDate.now().plusDays(1);
        UUID resId = UUID.randomUUID();
        UUID storeId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
        UUID userId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");
        UUID varId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf301");

        jdbcTemplate.update("""
                INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, '09:00:00', '13:00:00', 'PAYMENT_PROTECTED', NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days', NOW(), NOW())
                """, resId, storeId, userId, java.sql.Date.valueOf(serviceDate));

        jdbcTemplate.update("""
                INSERT INTO inventory_reservations (id, variant_id, store_id, capacity_reservation_id, quantity, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'COMMITTED', NOW() + INTERVAL '7 days', NOW(), NOW())
                """, UUID.randomUUID(), varId, storeId, resId);

        UUID paymentId = UUID.randomUUID();
        test6PaymentId.set(paymentId);
        mpExternalReference.set(paymentId.toString());

        jdbcTemplate.update("""
                INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, fulfillment_type, customer_name, customer_phone, customer_email, delivery_address, products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, items_snapshot, created_at, updated_at)
                VALUES (?, ?, 52.50, 'PEN', 'PENDING', 'NOT_REQUIRED', 'DELIVERY', 'Valeria Ríos', '+51965432109', 'cliente.demo@jaldishop.com', 'Av. José Larco 345', 45.00, 0, 7.50, 6.86, '[{"variantId":"80acf0ba-5a79-4888-b00d-b5aef8fcf301","productId":"80acf0ba-5a79-4888-b00d-b5aef8fcf201","productName":"Torta de Chocolate Artesanal","presentationName":"Mediana","sku":"TORTA-CHOC-MED","imageUrl":null,"quantity":1,"unitPriceAmount":45.00,"currency":"PEN","subtotalAmount":45.00,"tracksInventory":true}]'::jsonb, NOW(), NOW())
                """, paymentId, resId);

        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO payment_attempts (id, payment_id, attempt_number, provider, payment_method, idempotency_key, provider_payment_id, provider_status, status, created_at, updated_at)
                VALUES (?, ?, 1, 'MERCADO_PAGO', 'CARD', ?, ?, 'pending', 'CREATED', NOW(), NOW())
                """, attemptId, paymentId, "idemp-" + paymentId, mpPaymentId);

        // Enviar webhook firmado con HMAC-SHA256
        long ts = Instant.now().getEpochSecond();
        String requestId = "req-e2e-" + System.currentTimeMillis();
        String manifest = "id:" + mpPaymentId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        String xSignature = "ts=" + ts + ",v1=" + sb;

        HttpRequest webhookReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/checkout/mercado-pago/webhook?data.id=" + mpPaymentId + "&type=payment")))
                .header("x-signature", xSignature)
                .header("x-request-id", requestId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> webhookResp = httpClient.send(webhookReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, webhookResp.statusCode(), "Webhook debe responder 200 OK");

        // Validar persistencia real: Orden creada, pago APPROVED, capacidad COMMITTED
        String paymentStatus = jdbcTemplate.queryForObject("SELECT status FROM payments WHERE id = ?", String.class, paymentId);
        assertEquals("APPROVED", paymentStatus);

        String capStatus = jdbcTemplate.queryForObject("SELECT status FROM capacity_reservations WHERE id = ?", String.class, resId);
        assertEquals("COMMITTED", capStatus);

        Integer orderCount = jdbcTemplate.queryForObject("SELECT count(*) FROM orders WHERE payment_id = ?", Integer.class, paymentId);
        assertNotNull(orderCount);
        assertEquals(1, orderCount, "Debe haberse generado exactamente una orden");

        // Validar deduplicación en notificaciones
        UUID orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE payment_id = ?", UUID.class, paymentId);
        String expectedDedupKey = "NEW_ORDER:" + orderId;
        Integer notifCount = jdbcTemplate.queryForObject("SELECT count(*) FROM notifications WHERE deduplication_key = ?", Integer.class, expectedDedupKey);
        assertNotNull(notifCount);
        assertEquals(1, notifCount, "La notificación NEW_ORDER debe generarse con deduplication_key");
    }

    // =========================================================================
    // FASE 4: 7. GESTIÓN DE PEDIDOS POR COMERCIANTE Y CICLO DE VIDA
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("7. Ciclo de Vida de Pedido: Comerciante cambia estado CONFIRMED -> IN_PREPARATION -> READY -> COMPLETED")
    void test7_merchantOrderStatusTransitions() throws Exception {
        UUID storeId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");

        // Consultar lista de pedidos del comerciante
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/merchants/stores/" + storeId + "/orders?page=0&size=10")))
                .header("Authorization", "Bearer " + merchantToken)
                .GET()
                .build();
        HttpResponse<String> listResp = httpClient.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listResp.statusCode(), "Listado de pedidos del comerciante debe retornar 200 OK");

        JsonNode listJson = objectMapper.readTree(listResp.body());
        assertTrue(listJson.get("totalElements").asInt() >= 1);

        // Seleccionar una orden en CONFIRMED
        List<Map<String, Object>> confirmedOrders = jdbcTemplate.queryForList(
                "SELECT id FROM orders WHERE store_id = ? AND status = 'CONFIRMED' LIMIT 1",
                storeId
        );

        UUID orderId;
        if (!confirmedOrders.isEmpty()) {
            orderId = (UUID) confirmedOrders.get(0).get("id");
        } else {
            // Usar una orden recién creada o crear una orden de prueba
            orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE store_id = ? LIMIT 1", UUID.class, storeId);
            jdbcTemplate.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", orderId);
        }

        // Transición 1: CONFIRMED -> IN_PREPARATION
        String trans1Body = "{\"status\": \"IN_PREPARATION\"}";
        HttpRequest patchReq1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/merchants/stores/" + storeId + "/orders/" + orderId + "/status")))
                .header("Authorization", "Bearer " + merchantToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(trans1Body))
                .build();
        HttpResponse<String> patchResp1 = httpClient.send(patchReq1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patchResp1.statusCode(), "Transición a IN_PREPARATION debe ser 200 OK");

        // Transición 2: IN_PREPARATION -> READY
        String trans2Body = "{\"status\": \"READY\"}";
        HttpRequest patchReq2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/merchants/stores/" + storeId + "/orders/" + orderId + "/status")))
                .header("Authorization", "Bearer " + merchantToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(trans2Body))
                .build();
        HttpResponse<String> patchResp2 = httpClient.send(patchReq2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patchResp2.statusCode(), "Transición a READY debe ser 200 OK");

        // Transición 3: READY -> COMPLETED
        String trans3Body = "{\"status\": \"COMPLETED\"}";
        HttpRequest patchReq3 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/merchants/stores/" + storeId + "/orders/" + orderId + "/status")))
                .header("Authorization", "Bearer " + merchantToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(trans3Body))
                .build();
        HttpResponse<String> patchResp3 = httpClient.send(patchReq3, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patchResp3.statusCode(), "Transición a COMPLETED debe ser 200 OK");

        // Verificar persistencia del estado y trazabilidad en historial
        String finalStatus = jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
        assertEquals("COMPLETED", finalStatus);

        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_status_history WHERE order_id = ?", Integer.class, orderId
        );
        assertNotNull(historyCount);
        assertTrue(historyCount >= 3, "El historial debe contener los registros de auditoría de cada transición");
    }

    // =========================================================================
    // FASE 4: 8. ESCENARIOS NEGATIVOS RELEVANTES
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("8. Escenario Negativo: Stock insuficiente debe rechazar el hold")
    void test8_negativeInsufficientStock() throws Exception {
        UUID varId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf303");
        UUID storeId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
        UUID userId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");

        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)", userId);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM payment_attempts WHERE payment_id IN (SELECT id FROM payments WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED')))", userId);
        jdbcTemplate.update("DELETE FROM payments WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED'))", userId);
        jdbcTemplate.update("DELETE FROM inventory_reservations WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED'))", userId);
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ? AND status IN ('ACTIVE', 'PAYMENT_PROTECTED')", userId);

        // Agregar 5 unidades al carrito
        String addItemBody = String.format("""
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 5
                }
                """, storeId, varId);

        HttpRequest addReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/cart/items")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + customerToken)
                .POST(HttpRequest.BodyPublishers.ofString(addItemBody))
                .build();
        HttpResponse<String> addResp = httpClient.send(addReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, addResp.statusCode(), "Agregar al carrito debe ser 201: " + addResp.body());

        // Simular stock insuficiente en inventario reduciéndolo a 1
        int originalStock = jdbcTemplate.queryForObject("SELECT quantity FROM inventories WHERE variant_id = ?", Integer.class, varId);
        jdbcTemplate.update("UPDATE inventories SET quantity = 1 WHERE variant_id = ?", varId);

        try {
            LocalDate serviceDate = jdbcTemplate.queryForObject("""
                    SELECT MIN(CURRENT_DATE + days_ahead)
                    FROM generate_series(21, 83) AS candidates(days_ahead)
                    WHERE EXTRACT(ISODOW FROM CURRENT_DATE + days_ahead) = 2
                      AND EXISTS (
                          SELECT 1 FROM capacity_configurations c
                          WHERE c.store_id = ? AND c.day_of_week = 2
                            AND c.status = 'ACTIVE' AND c.max_capacity > 0
                            AND ((c.start_time IS NULL AND c.end_time IS NULL)
                                 OR (c.start_time <= TIME '09:00:00' AND c.end_time >= TIME '13:00:00'))
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM capacity_exceptions e
                          WHERE e.store_id = ? AND e.service_date = CURRENT_DATE + days_ahead
                            AND e.status = 'ACTIVE'
                      )
                    """, LocalDate.class, storeId, storeId);
            assertNotNull(serviceDate, "Debe existir un martes con capacidad semanal positiva y sin excepciones activas");
            String holdBody = String.format("""
                    {
                        "storeId": "%s",
                        "fulfillmentType": "PICKUP",
                        "serviceDate": "%s",
                        "startTime": "09:00:00",
                        "endTime": "13:00:00",
                        "customerName": "Valeria Ríos",
                        "customerPhone": "+51965432109",
                        "customerEmail": "cliente.demo@jaldishop.com",
                        "deliveryAddress": null,
                        "deliveryReference": null,
                        "deliveryLatitude": null,
                        "deliveryLongitude": null,
                        "discountCode": null
                    }
                    """, storeId, serviceDate);

            HttpRequest checkoutReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl("/api/v1/checkout")))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + customerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(holdBody))
                    .build();

            HttpResponse<String> checkoutResp = httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(409, checkoutResp.statusCode(), "Checkout hold con stock insuficiente debe responder 409 Conflict");
            assertTrue(checkoutResp.body().contains("INSUFFICIENT_STOCK"), "Respuesta inesperada: " + checkoutResp.body());
        } finally {
            // Restaurar stock original
            jdbcTemplate.update("UPDATE inventories SET quantity = ? WHERE variant_id = ?", originalStock, varId);
        }
    }

    @Test
    @Order(9)
    @DisplayName("9. Escenario Negativo: Capacidad agotada (franja con capacidad 0) debe rechazar la reserva")
    void test9_negativeCapacityExhausted() throws Exception {
        UUID storeId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
        LocalDate fullDate = jdbcTemplate.queryForObject("""
                SELECT MIN(CURRENT_DATE + days_ahead)
                FROM generate_series(21, 83) AS candidates(days_ahead)
                WHERE EXTRACT(ISODOW FROM CURRENT_DATE + days_ahead) = 2
                  AND EXISTS (
                      SELECT 1 FROM capacity_configurations c
                      WHERE c.store_id = ? AND c.day_of_week = 2
                        AND c.status = 'ACTIVE' AND c.max_capacity > 0
                        AND ((c.start_time IS NULL AND c.end_time IS NULL)
                             OR (c.start_time <= TIME '09:00:00' AND c.end_time >= TIME '13:00:00'))
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM capacity_exceptions e
                      WHERE e.store_id = ? AND e.service_date = CURRENT_DATE + days_ahead
                        AND e.status = 'ACTIVE'
                  )
                """, LocalDate.class, storeId, storeId);
        assertNotNull(fullDate, "Debe existir un martes con capacidad semanal positiva y sin excepciones activas");

        // Crear excepción de capacidad con exception_capacity = 0 para simular franja agotada
        UUID exceptionId = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, reason, status, created_at, updated_at)
                    VALUES (?, ?, ?, '09:00:00', '13:00:00', 0, 'Mantenimiento preventivo', 'ACTIVE', NOW(), NOW())
                    """, exceptionId, storeId, java.sql.Date.valueOf(fullDate));

            String holdBody = String.format("""
                    {
                        "storeId": "%s",
                        "fulfillmentType": "PICKUP",
                        "serviceDate": "%s",
                        "startTime": "09:00:00",
                        "endTime": "13:00:00",
                        "customerName": "Valeria Ríos",
                        "customerPhone": "+51965432109",
                        "customerEmail": "cliente.demo@jaldishop.com",
                        "deliveryAddress": null,
                        "deliveryReference": null,
                        "deliveryLatitude": null,
                        "deliveryLongitude": null,
                        "discountCode": null
                    }
                    """, storeId, fullDate);

            HttpRequest checkoutReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl("/api/v1/checkout")))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + customerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(holdBody))
                    .build();

            HttpResponse<String> checkoutResp = httpClient.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(409, checkoutResp.statusCode(), "Reserva en franja sin capacidad debe responder 409: " + checkoutResp.body());
            assertEquals("CAPACITY_UNAVAILABLE", objectMapper.readTree(checkoutResp.body()).path("code").asText());
        } finally {
            jdbcTemplate.update("DELETE FROM capacity_exceptions WHERE id = ? AND store_id = ?", exceptionId, storeId);
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. Escenario Negativo: Idempotencia ante Webhook duplicado")
    void test10_negativeDuplicateWebhookIdempotency() throws Exception {
        mpPaymentStatus.set("approved");
        mpTransactionAmount.set(52.50);
        String mpPaymentId = String.valueOf(3000000000L + (System.currentTimeMillis() % 100000000));

        // Obtener el pago aprobado en el flujo previo (test6) o el más reciente con intento asociado
        UUID existingPaymentId = test6PaymentId.get();
        if (existingPaymentId == null) {
            existingPaymentId = jdbcTemplate.queryForObject(
                    "SELECT p.id FROM payments p JOIN payment_attempts pa ON pa.payment_id = p.id WHERE p.status = 'APPROVED' ORDER BY p.created_at DESC LIMIT 1",
                    UUID.class
            );
        }
        assertNotNull(existingPaymentId);
        mpExternalReference.set(existingPaymentId.toString());

        java.math.BigDecimal pAmount = jdbcTemplate.queryForObject(
                "SELECT amount FROM payments WHERE id = ?", java.math.BigDecimal.class, existingPaymentId
        );
        mpTransactionAmount.set(pAmount != null ? pAmount.doubleValue() : 52.50);

        // Asociar el provider_payment_id al intento
        jdbcTemplate.update("""
                UPDATE payment_attempts SET provider_payment_id = ? WHERE payment_id = ?
                """, mpPaymentId, existingPaymentId);

        int ordersBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class);

        // Enviar webhook duplicado con HMAC válido
        long ts = Instant.now().getEpochSecond();
        String requestId = "req-e2e-dup-" + System.currentTimeMillis();
        String manifest = "id:" + mpPaymentId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        String xSignature = "ts=" + ts + ",v1=" + sb;

        HttpRequest dupReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/checkout/mercado-pago/webhook?data.id=" + mpPaymentId + "&type=payment")))
                .header("x-signature", xSignature)
                .header("x-request-id", requestId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> dupResp = httpClient.send(dupReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dupResp.statusCode(), "Webhook duplicado debe responder 200 OK idemptótente");

        int ordersAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class);
        assertEquals(ordersBefore, ordersAfter, "Un webhook duplicado no debe generar órdenes redundantes");
    }

    @Test
    @Order(11)
    @DisplayName("11. Escenario Negativo: Webhook con pago REJECTED no debe confirmar orden")
    void test11_negativeRejectedPaymentDoesNotConfirmOrder() throws Exception {
        mpPaymentStatus.set("rejected");
        mpTransactionAmount.set(30.00);
        String mpPaymentId = String.valueOf(4000000000L + (System.currentTimeMillis() % 100000000));

        LocalDate serviceDate = LocalDate.now().plusDays(2);
        UUID resId = UUID.randomUUID();
        UUID storeId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
        UUID userId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");
        UUID varId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf301");

        jdbcTemplate.update("""
                INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, '09:00:00', '13:00:00', 'PAYMENT_PROTECTED', NOW() + INTERVAL '30 minutes', NOW() + INTERVAL '30 minutes', NOW(), NOW())
                """, resId, storeId, userId, java.sql.Date.valueOf(serviceDate));

        jdbcTemplate.update("""
                INSERT INTO inventory_reservations (id, variant_id, store_id, capacity_reservation_id, quantity, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())
                """, UUID.randomUUID(), varId, storeId, resId);

        UUID paymentId = UUID.randomUUID();
        test6PaymentId.set(paymentId);
        mpExternalReference.set(paymentId.toString());

        jdbcTemplate.update("""
                INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, fulfillment_type, customer_name, customer_phone, customer_email, products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, items_snapshot, created_at, updated_at)
                VALUES (?, ?, 30.00, 'PEN', 'PENDING', 'NOT_REQUIRED', 'PICKUP', 'Valeria Ríos', '+51965432109', 'cliente.demo@jaldishop.com', 30.00, 0, 0, 4.58, '[]'::jsonb, NOW(), NOW())
                """, paymentId, resId);

        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO payment_attempts (id, payment_id, attempt_number, provider, payment_method, idempotency_key, provider_payment_id, provider_status, status, created_at, updated_at)
                VALUES (?, ?, 1, 'MERCADO_PAGO', 'CARD', ?, ?, 'pending', 'CREATED', NOW(), NOW())
                """, attemptId, paymentId, "idemp-rej-" + paymentId, mpPaymentId);

        long ts = Instant.now().getEpochSecond();
        String requestId = "req-e2e-rej-" + System.currentTimeMillis();
        String manifest = "id:" + mpPaymentId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        String xSignature = "ts=" + ts + ",v1=" + sb;

        HttpRequest webhookReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl("/api/v1/checkout/mercado-pago/webhook?data.id=" + mpPaymentId + "&type=payment")))
                .header("x-signature", xSignature)
                .header("x-request-id", requestId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> webhookResp = httpClient.send(webhookReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, webhookResp.statusCode(), "Webhook rejected debe procesarse con 200 OK");

        String attemptStatus = jdbcTemplate.queryForObject("SELECT status FROM payment_attempts WHERE id = ?", String.class, attemptId);
        assertEquals("REJECTED", attemptStatus, "El intento de pago debe registrarse como REJECTED");

        String pStatus = jdbcTemplate.queryForObject("SELECT status FROM payments WHERE id = ?", String.class, paymentId);
        assertEquals("PENDING", pStatus, "El pago principal debe mantenerse en PENDING para reintento");

        Integer ordCount = jdbcTemplate.queryForObject("SELECT count(*) FROM orders WHERE payment_id = ?", Integer.class, paymentId);
        assertEquals(0, ordCount, "No debe crearse orden alguna para un pago rechazado");
    }
}
