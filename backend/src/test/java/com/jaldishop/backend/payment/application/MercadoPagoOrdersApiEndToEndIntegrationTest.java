package com.jaldishop.backend.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceCommand;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceResult;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceService;
import com.jaldishop.backend.payment.application.refund.PaymentRefundService;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookCommand;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookResult;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("MercadoPagoOrdersApiEndToEndIntegrationTest — 15 Escenarios E2E Simulados")
class MercadoPagoOrdersApiEndToEndIntegrationTest {

    private static HttpServer mockServer;
    private static ExecutorService mockExecutor;
    private static int mockPort;

    private static final String WEBHOOK_SECRET = "orders-webhook-secret-key-4a2";
    private static final Long EXPECTED_COLLECTOR_ID = 123456789L;

    private static final AtomicReference<MockResponse> ordersMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockResponse> preferencesMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockResponse> paymentsMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockResponse> refundsMockResponse = new AtomicReference<>();
    private static final AtomicBoolean simulateTimeout = new AtomicBoolean(false);
    private static final List<String> receivedRequestBodyList = new CopyOnWriteArrayList<>();

    record MockResponse(int statusCode, String body) {}

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockExecutor = Executors.newCachedThreadPool();
        mockServer.setExecutor(mockExecutor);

        // Handler genérico de despacho para Mercado Pago
        mockServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] reqBytes = exchange.getRequestBody().readAllBytes();
                String reqBody = new String(reqBytes, StandardCharsets.UTF_8);
                if (!reqBody.isBlank()) {
                    receivedRequestBodyList.add(reqBody);
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);

                if (simulateTimeout.get()) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                MockResponse resp = null;
                if (path.startsWith("/v1/orders")) {
                    resp = ordersMockResponse.get();
                    if (resp == null) {
                        resp = new MockResponse(200, "{\"id\": \"ORD-DEFAULT\", \"status\": \"created\"}");
                    }
                } else if (path.startsWith("/checkout/preferences")) {
                    resp = preferencesMockResponse.get();
                    if (resp == null) {
                        resp = new MockResponse(201, "{\"id\": \"PREF-DEFAULT\", \"init_point\": \"https://mp.com/init\"}");
                    }
                } else if (path.contains("/refunds")) {
                    resp = refundsMockResponse.get();
                    if (resp == null) {
                        resp = "GET".equals(method) ? new MockResponse(200, "[]") : new MockResponse(201, "{\"id\": 111, \"status\": \"approved\"}");
                    }
                } else if (path.startsWith("/v1/payments/")) {
                    resp = paymentsMockResponse.get();
                    if (resp == null) {
                        resp = new MockResponse(200, "{\"id\": 222, \"status\": \"approved\"}");
                    }
                } else {
                    resp = new MockResponse(404, "{\"message\": \"Not Found\"}");
                }

                byte[] bytes = resp.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(resp.statusCode(), bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        if (mockExecutor != null) {
            mockExecutor.shutdownNow();
        }
    }

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @DynamicPropertySource
    static void configureIsolatedProperties(DynamicPropertyRegistry registry) {
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
        registry.add("mercadopago.access-token", () -> "TEST-ORDERS-TOKEN");
        registry.add("mercadopago.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("mercadopago.expected-collector-id", () -> String.valueOf(EXPECTED_COLLECTOR_ID));
        registry.add("mercadopago.expected-live-mode", () -> "false");
        registry.add("mercadopago.connect-timeout-ms", () -> "600");
        registry.add("mercadopago.read-timeout-ms", () -> "600");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
    }

    @Autowired private CreatePaymentPreferenceService createPaymentPreferenceService;
    @Autowired private ProcessMercadoPagoWebhookService webhookService;
    @Autowired private PaymentRefundService refundService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private PaymentIncidenceRepository incidenceRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MercadoPagoProperties properties;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");
    private static final UUID VARIANT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302");

    @BeforeEach
    void setupAndClean() {
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.PREFERENCES);
        simulateTimeout.set(false);
        receivedRequestBodyList.clear();
        ordersMockResponse.set(null);
        preferencesMockResponse.set(null);
        paymentsMockResponse.set(null);
        refundsMockResponse.set(null);

        jdbcTemplate.update("DELETE FROM payment_refund_execution_attempts");
        jdbcTemplate.update("DELETE FROM payment_refund_intents");
        jdbcTemplate.update("DELETE FROM payment_incidences");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM inventory_reservations");
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("UPDATE inventories SET quantity = 100 WHERE variant_id = ?", VARIANT_ID);
        createCartWithItem(USER_ID);
    }

    private void createCartWithItem(UUID userId) {
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)", userId);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", userId);
        UUID cartId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO carts (id, user_id, store_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                cartId, userId, STORE_ID
        );
        jdbcTemplate.update(
                "INSERT INTO cart_items (cart_id, variant_id, store_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 1, 12.50, 'PEN', NOW(), NOW())",
                cartId, VARIANT_ID, STORE_ID
        );
    }

    private String calculateHmac(String dataId, String requestId, long ts) {
        String manifest = "id:" + dataId.trim().toLowerCase(Locale.ROOT) + ";request-id:" + requestId.trim() + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return "ts=" + ts + ",v1=" + sb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UUID createCapacityReservation(String status, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        Instant protectionExpires = "PAYMENT_PROTECTED".equals(status) ? Instant.now().plusSeconds(1800) : null;
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, Date.valueOf(LocalDate.now().plusDays(1)),
                Time.valueOf(LocalTime.of(10, 0)), Time.valueOf(LocalTime.of(11, 0)),
                status, java.sql.Timestamp.from(expiresAt != null ? expiresAt : Instant.now().plusSeconds(1800)),
                protectionExpires != null ? java.sql.Timestamp.from(protectionExpires) : null,
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        );
        return reservationId;
    }

    private UUID createActiveInventoryReservation(UUID capacityReservationId, int quantity) {
        UUID inventoryReservationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())",
                inventoryReservationId, STORE_ID, capacityReservationId, VARIANT_ID, quantity
        );
        return inventoryReservationId;
    }

    private Payment createPendingPayment(UUID reservationId, BigDecimal amount, String snapshot) {
        Payment payment = Payment.createPending(
                reservationId, amount, "PEN",
                FulfillmentType.DELIVERY, "Cliente E2E", "987654321", "cliente.e2e@test.com",
                "Calle Los Sauces 123", null, null, null, amount, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, snapshot != null ? snapshot : "[]"
        );
        return paymentRepository.save(payment);
    }

    private String buildValidSnapshot(BigDecimal amount) {
        return String.format(Locale.ROOT, """
                [{"variantId":"%s","productId":"%s","productName":"Pastel","presentationName":"Familiar","sku":"PASTEL-FAM-01","imageUrl":null,"quantity":1,"unitPriceAmount":%.2f,"currency":"PEN","subtotalAmount":%.2f,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID, amount, amount);
    }

    // =========================================================================
    // E2E-01: Creación exitosa de una orden mediante Orders API
    // =========================================================================
    @Test
    @DisplayName("E2E-01: Creación exitosa de una orden mediante Orders API con config.online")
    void testE2E01_successfulOrderCreationWithOrdersApi() {
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.ORDERS);

        UUID reservationId = createCapacityReservation("ACTIVE", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);

        ordersMockResponse.set(new MockResponse(201, """
                {
                  "id": "ORD-E2E-001",
                  "status": "created",
                  "checkout_url": "https://www.mercadopago.com.pe/checkout/v1/redirect?order_id=ORD-E2E-001",
                  "total_amount": 25.00
                }
                """));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                USER_ID, reservationId, CheckoutFulfillmentType.DELIVERY,
                "Cliente E2E 01", "987654321", "e2e01@test.com",
                "Av. Principal 456", "Frente al parque", null, null
        );

        CreatePaymentPreferenceResult result = createPaymentPreferenceService.execute(command);

        assertThat(result.preferenceId()).isEqualTo("ORD-E2E-001");
        assertThat(result.initPoint()).contains("order_id=ORD-E2E-001");

        // Verificación de base de datos
        Payment payment = paymentRepository.findById(result.paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
        assertThat(attempts).hasSize(1);
        PaymentAttempt attempt = attempts.get(0);
        assertThat(attempt.getCheckoutMode()).isEqualTo("ORDERS");
        assertThat(attempt.getProviderOrderId()).isEqualTo("ORD-E2E-001");
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);

        // Verificación del request enviado al mock HTTP
        assertThat(receivedRequestBodyList).isNotEmpty();
        String sentBody = receivedRequestBodyList.get(receivedRequestBodyList.size() - 1);
        assertThat(sentBody).contains("\"type\":\"online\"");
        assertThat(sentBody).contains("\"processing_mode\":\"manual\"");
        assertThat(sentBody).contains("\"config\":{\"online\":");
        assertThat(sentBody).doesNotContain("\"back_urls\"");
        assertThat(sentBody).doesNotContain("\"notification_url\"");
    }

    // =========================================================================
    // E2E-02: Regresión del flujo legacy Preferences
    // =========================================================================
    @Test
    @DisplayName("E2E-02: Regresión del flujo legacy Preferences sin alteraciones en su contrato")
    void testE2E02_legacyPreferencesFlowRegression() {
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.PREFERENCES);

        UUID reservationId = createCapacityReservation("ACTIVE", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);

        preferencesMockResponse.set(new MockResponse(201, """
                {
                  "id": "PREF-E2E-002",
                  "init_point": "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=PREF-E2E-002",
                  "sandbox_init_point": "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=PREF-E2E-002"
                }
                """));

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                USER_ID, reservationId, CheckoutFulfillmentType.DELIVERY,
                "Cliente Legacy", "987654321", "legacy@test.com",
                "Av. Principal 456", "Frente al parque", null, null
        );

        CreatePaymentPreferenceResult result = createPaymentPreferenceService.execute(command);

        assertThat(result.preferenceId()).isEqualTo("PREF-E2E-002");
        assertThat(result.initPoint()).contains("pref_id=PREF-E2E-002");

        Payment payment = paymentRepository.findById(result.paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
        PaymentAttempt attempt = attempts.get(0);
        assertThat(attempt.getCheckoutMode()).isEqualTo("PREFERENCES");
        assertThat(attempt.getPreferenceId()).isEqualTo("PREF-E2E-002");
        assertThat(attempt.getProviderOrderId()).isNull();
    }

    // =========================================================================
    // E2E-03: Orden creada sin transacciones financieras
    // =========================================================================
    @Test
    @DisplayName("E2E-03: Orden creada sin transacciones no es aprobada falsamente y se ignora con seguridad")
    void testE2E03_orderCreatedWithoutFinancialTransactions() {
        BigDecimal amount = BigDecimal.valueOf(30.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-03", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-003", "https://mp.com/checkout?order_id=ORD-E2E-003");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-003",
                  "status": "created",
                  "total_amount": 30.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": []
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String requestId = UUID.randomUUID().toString();
        String xSignature = calculateHmac("ORD-E2E-003", requestId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", "ORD-E2E-003", "type", "order"),
                "{\"action\": \"order.created\", \"data\": {\"id\": \"ORD-E2E-003\"}}"
        );

        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertThat(result.processed()).isFalse();
        assertThat(result.message()).contains("sin transacciones de pago");

        Payment paymentDb = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentDb.getStatus()).isEqualTo(PaymentStatus.PENDING);

        int orderCount = jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class);
        assertThat(orderCount).isZero();
    }

    // =========================================================================
    // E2E-04: Pago pendiente o en procesamiento sin acreditación
    // =========================================================================
    @Test
    @DisplayName("E2E-04: Pago en estado in_process no confirma comercialmente y transiciona a PROCESSING")
    void testE2E04_orderWithPendingPayment_transitionsToProcessing() {
        BigDecimal amount = BigDecimal.valueOf(35.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-04", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-004", "https://mp.com/checkout?order_id=ORD-E2E-004");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-004",
                  "status": "processing",
                  "total_amount": 35.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY-E2E-004",
                        "status": "in_process",
                        "status_detail": "pending_review_manual",
                        "transaction_amount": 35.00,
                        "currency_id": "PEN",
                        "payment_method_id": "visa",
                        "payment_type_id": "credit_card"
                      }
                    ]
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String requestId = UUID.randomUUID().toString();
        String xSignature = calculateHmac("ORD-E2E-004", requestId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", "ORD-E2E-004", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-004\"}}"
        );

        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("PROCESSING");

        Payment paymentDb = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentDb.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        PaymentAttempt attemptDb = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(attemptDb.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(attemptDb.getProviderStatus()).isEqualTo("in_process");
        assertThat(attemptDb.getProviderPaymentId()).isEqualTo("PAY-E2E-004");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    // =========================================================================
    // E2E-05: Pago acreditado y confirmación del pedido comercial
    // =========================================================================
    @Test
    @DisplayName("E2E-05: Pago acreditado confirma comercialmente el pedido y compromete inventario")
    void testE2E05_accreditedPayment_confirmsCommercialOrder() {
        BigDecimal amount = BigDecimal.valueOf(40.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        UUID inventoryReservationId = createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-05", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-005", "https://mp.com/checkout?order_id=ORD-E2E-005");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-005",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 40.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY-E2E-005",
                        "status": "processed",
                        "status_detail": "accredited",
                        "transaction_amount": 40.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T12:00:00.000Z",
                        "payment_method_id": "visa",
                        "payment_type_id": "credit_card"
                      }
                    ]
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String requestId = UUID.randomUUID().toString();
        String xSignature = calculateHmac("ORD-E2E-005", requestId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", "ORD-E2E-005", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-005\"}}"
        );

        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");

        Payment paymentDb = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentDb.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        PaymentAttempt attemptDb = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(attemptDb.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(attemptDb.getProviderPaymentId()).isEqualTo("PAY-E2E-005");

        Order order = orderRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(amount);

        String capStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM capacity_reservations WHERE id = ?", String.class, reservationId
        );
        assertThat(capStatus).isEqualTo("COMMITTED");

        String invStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM inventory_reservations WHERE id = ?", String.class, inventoryReservationId
        );
        assertThat(invStatus).isEqualTo("COMMITTED");
    }

    // =========================================================================
    // E2E-06: Webhook duplicado e idempotencia
    // =========================================================================
    @Test
    @DisplayName("E2E-06: Webhook duplicado no genera órdenes duplicadas ni altera estado APPROVED")
    void testE2E06_duplicateWebhook_appliesIdempotentlyWithoutDegradation() {
        testE2E05_accreditedPayment_confirmsCommercialOrder();

        long ts = Instant.now().getEpochSecond();
        String secondRequestId = UUID.randomUUID().toString();
        String xSignature = calculateHmac("ORD-E2E-005", secondRequestId, ts);

        ProcessMercadoPagoWebhookCommand secondCommand = new ProcessMercadoPagoWebhookCommand(
                xSignature, secondRequestId, Map.of("data.id", "ORD-E2E-005", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-005\"}}"
        );

        ProcessMercadoPagoWebhookResult secondResult = webhookService.execute(secondCommand);

        assertThat(secondResult.processed()).isTrue();
        assertThat(secondResult.paymentStatus()).isEqualTo("APPROVED");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
    }

    // =========================================================================
    // E2E-07: Webhooks concurrentes sobre el mismo pago
    // =========================================================================
    @Test
    @DisplayName("E2E-07: Webhooks concurrentes se serializan vía bloqueo pesimista generando exactamente 1 pedido")
    void testE2E07_concurrentWebhooks_serializesViaPessimisticLock() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-07", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-007", "https://mp.com/checkout?order_id=ORD-E2E-007");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-007",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 50.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY-E2E-007",
                        "status": "processed",
                        "status_detail": "accredited",
                        "transaction_amount": 50.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T12:00:00.000Z",
                        "payment_method_id": "visa",
                        "payment_type_id": "credit_card"
                      }
                    ]
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String req1 = UUID.randomUUID().toString();
        String req2 = UUID.randomUUID().toString();
        String sig1 = calculateHmac("ORD-E2E-007", req1, ts);
        String sig2 = calculateHmac("ORD-E2E-007", req2, ts);

        ProcessMercadoPagoWebhookCommand cmd1 = new ProcessMercadoPagoWebhookCommand(
                sig1, req1, Map.of("data.id", "ORD-E2E-007", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-007\"}}"
        );
        ProcessMercadoPagoWebhookCommand cmd2 = new ProcessMercadoPagoWebhookCommand(
                sig2, req2, Map.of("data.id", "ORD-E2E-007", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-007\"}}"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<ProcessMercadoPagoWebhookResult> f1 = executor.submit(() -> webhookService.execute(cmd1));
        Future<ProcessMercadoPagoWebhookResult> f2 = executor.submit(() -> webhookService.execute(cmd2));

        ProcessMercadoPagoWebhookResult res1 = f1.get(10, TimeUnit.SECONDS);
        ProcessMercadoPagoWebhookResult res2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(res1.paymentStatus()).isEqualTo("APPROVED");
        assertThat(res2.paymentStatus()).isEqualTo("APPROVED");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(1);
    }

    // =========================================================================
    // E2E-08: Webhook con firma HMAC inválida
    // =========================================================================
    @Test
    @DisplayName("E2E-08: Webhook con firma HMAC inválida es rechazado sin modificar BD")
    void testE2E08_invalidHmacSignature_rejectedWithSecurityException() {
        String badSignature = "ts=" + Instant.now().getEpochSecond() + ",v1=invalid_tampered_hash_123456";
        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                badSignature, "req-bad-sig", Map.of("data.id", "ORD-TAMPERED", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-TAMPERED\"}}"
        );

        assertThrows(SecurityException.class, () -> webhookService.execute(command));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    // =========================================================================
    // E2E-09: Webhook con identificadores de otra orden o pago
    // =========================================================================
    @Test
    @DisplayName("E2E-09: Webhook con external_reference no existente en BD lanza ResourceNotFoundException")
    void testE2E09_webhookWithMismatchedOrNonExistentExternalReference() {
        UUID nonExistentPaymentId = UUID.randomUUID();
        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-MISMATCH-09",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 10.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY-MISMATCH-09",
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 10.00,
                        "currency_id": "PEN"
                      }
                    ]
                  }
                }
                """, nonExistentPaymentId)));

        long ts = Instant.now().getEpochSecond();
        String reqId = UUID.randomUUID().toString();
        String sig = calculateHmac("ORD-MISMATCH-09", reqId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                sig, reqId, Map.of("data.id", "ORD-MISMATCH-09", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-MISMATCH-09\"}}"
        );

        assertThrows(ResourceNotFoundException.class, () -> webhookService.execute(command));
    }

    // =========================================================================
    // E2E-10: Pago acreditado después de expirar la reserva
    // =========================================================================
    @Test
    @DisplayName("E2E-10: Pago acreditado con reserva vencida preserva APPROVED y registra incidencia duradera")
    void testE2E10_paymentAccreditedAfterCapacityReservationExpired_recordsIncidence() {
        BigDecimal amount = BigDecimal.valueOf(60.00);
        // Reserva expirada en el pasado
        UUID reservationId = createCapacityReservation("EXPIRED", Instant.now().minusSeconds(600));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-10", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-010", "https://mp.com/checkout?order_id=ORD-E2E-010");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-010",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 60.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "123456789",
                        "status": "processed",
                        "status_detail": "accredited",
                        "transaction_amount": 60.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T12:00:00.000Z"
                      }
                    ]
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String reqId = UUID.randomUUID().toString();
        String sig = calculateHmac("ORD-E2E-010", reqId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                sig, reqId, Map.of("data.id", "ORD-E2E-010", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-010\"}}"
        );

        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertThat(result.processed()).isTrue();
        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.message()).contains("La reserva de capacidad expiró");

        Payment paymentDb = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentDb.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentDb.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(paymentDb.getRefundReference()).contains("INCIDENCE:La reserva de capacidad expiró");

        // Sin pedido comercial confirmado
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    // =========================================================================
    // E2E-11: Fallo entre acreditación financiera y confirmación comercial
    // =========================================================================
    @Test
    @DisplayName("E2E-11: Fallo comercial por reserva liberada preserva APPROVED y registra incidencia")
    void testE2E11_failureBetweenFinancialAccreditationAndCommercialConfirmation() {
        BigDecimal amount = BigDecimal.valueOf(70.00);
        UUID reservationId = createCapacityReservation("RELEASED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-e2e-11", "ORDERS"
        );
        attempt.recordOrder("ORD-E2E-011", "https://mp.com/checkout?order_id=ORD-E2E-011");
        attempt.markProcessing();
        paymentAttemptRepository.save(attempt);

        ordersMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": "ORD-E2E-011",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 70.00,
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "transactions": {
                    "payments": [
                      {
                        "id": "PAY-E2E-011",
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 70.00,
                        "currency_id": "PEN"
                      }
                    ]
                  }
                }
                """, payment.getId())));

        long ts = Instant.now().getEpochSecond();
        String reqId = UUID.randomUUID().toString();
        String sig = calculateHmac("ORD-E2E-011", reqId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                sig, reqId, Map.of("data.id", "ORD-E2E-011", "type", "order"),
                "{\"action\": \"order.processed\", \"data\": {\"id\": \"ORD-E2E-011\"}}"
        );

        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        Payment paymentDb = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentDb.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentDb.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    // =========================================================================
    // E2E-12: Recuperación mediante scheduler de reconciliación / reembolsos
    // =========================================================================
    @Test
    @DisplayName("E2E-12: Recuperación mediante procesamiento de reembolsos pendientes emite reembolso y actualiza a COMPLETED")
    void testE2E12_recoveryViaReconciliationScheduler() {
        testE2E10_paymentAccreditedAfterCapacityReservationExpired_recordsIncidence();

        UUID paymentId = jdbcTemplate.queryForObject(
                "SELECT id FROM payments WHERE refund_status = 'PENDING' LIMIT 1", UUID.class
        );
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        refundsMockResponse.set(new MockResponse(201, """
                {
                  "id": 99008812,
                  "payment_id": 123456789,
                  "amount": 60.00,
                  "status": "approved",
                  "date_created": "2026-11-05T12:30:00.000Z"
                }
                """));

        var stats = refundService.processPendingRefunds(10);

        assertThat(stats.completedCount()).isEqualTo(1);

        Payment paymentAfter = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(paymentAfter.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
    }

    // =========================================================================
    // E2E-13: Timeout durante la creación de una orden remota
    // =========================================================================
    @Test
    @DisplayName("E2E-13: Timeout durante POST /v1/orders traduce a MP_TIMEOUT, registra ERROR en intento y permite reintento")
    void testE2E13_timeoutDuringOrderCreation_recordsErrorAndPermitsRecovery() {
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.ORDERS);

        UUID reservationId = createCapacityReservation("ACTIVE", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);

        simulateTimeout.set(true);

        CreatePaymentPreferenceCommand command = new CreatePaymentPreferenceCommand(
                USER_ID, reservationId, CheckoutFulfillmentType.DELIVERY,
                "Cliente Timeout", "987654321", "timeout@test.com",
                "Calle 1", "Ref", null, null
        );

        MercadoPagoException ex = assertThrows(MercadoPagoException.class, () -> createPaymentPreferenceService.execute(command));
        assertThat(ex.getErrorCode()).isEqualTo("MP_TIMEOUT");

        Payment payment = paymentRepository.findByCapacityReservationId(reservationId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        List<PaymentAttempt> attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
        assertThat(attempts).hasSize(1);
        PaymentAttempt attempt = attempts.get(0);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.ERROR);
        assertThat(attempt.getErrorCode()).isEqualTo("MP_TIMEOUT");

        // Recuperación: Se apaga el timeout y el cliente reintenta
        simulateTimeout.set(false);
        ordersMockResponse.set(new MockResponse(201, """
                {
                  "id": "ORD-RECOVERED-13",
                  "status": "created",
                  "checkout_url": "https://mp.com/checkout?order_id=ORD-RECOVERED-13",
                  "total_amount": 25.00
                }
                """));

        CreatePaymentPreferenceResult retryResult = createPaymentPreferenceService.execute(command);
        assertThat(retryResult.preferenceId()).isEqualTo("ORD-RECOVERED-13");

        List<PaymentAttempt> attemptsAfter = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
        assertThat(attemptsAfter).hasSize(1);
        PaymentAttempt recoveredAttempt = attemptsAfter.get(0);
        assertThat(recoveredAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(recoveredAttempt.getProviderOrderId()).isEqualTo("ORD-RECOVERED-13");
    }

    // =========================================================================
    // E2E-14: Cambio de feature flag entre PREFERENCES y ORDERS
    // =========================================================================
    @Test
    @DisplayName("E2E-14: Cambio de feature flag entre PREFERENCES y ORDERS opera limpiamente en ambos modos")
    void testE2E14_featureFlagSwitchBetweenPreferencesAndOrders() {
        // 1. Pago A bajo PREFERENCES
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.PREFERENCES);
        UUID resA = createCapacityReservation("ACTIVE", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(resA, 1);
        preferencesMockResponse.set(new MockResponse(201, "{\"id\": \"PREF-A\", \"init_point\": \"https://mp.com/A\"}"));

        CreatePaymentPreferenceCommand cmdA = new CreatePaymentPreferenceCommand(
                USER_ID, resA, CheckoutFulfillmentType.DELIVERY,
                "Cliente A", "987654321", "a@test.com", "Dir A", null, null, null
        );
        CreatePaymentPreferenceResult resPrefA = createPaymentPreferenceService.execute(cmdA);

        // 2. Cambio dinámico a ORDERS
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.ORDERS);
        UUID resB = createCapacityReservation("ACTIVE", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(resB, 1);
        ordersMockResponse.set(new MockResponse(201, "{\"id\": \"ORD-B\", \"checkout_url\": \"https://mp.com/B\"}"));

        CreatePaymentPreferenceCommand cmdB = new CreatePaymentPreferenceCommand(
                USER_ID, resB, CheckoutFulfillmentType.DELIVERY,
                "Cliente B", "987654321", "b@test.com", "Dir B", null, null, null
        );
        CreatePaymentPreferenceResult resOrderB = createPaymentPreferenceService.execute(cmdB);

        PaymentAttempt attemptA = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(resPrefA.paymentId()).get(0);
        PaymentAttempt attemptB = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(resOrderB.paymentId()).get(0);

        assertThat(attemptA.getCheckoutMode()).isEqualTo("PREFERENCES");
        assertThat(attemptA.getPreferenceId()).isEqualTo("PREF-A");

        assertThat(attemptB.getCheckoutMode()).isEqualTo("ORDERS");
        assertThat(attemptB.getProviderOrderId()).isEqualTo("ORD-B");

        // Restauración a PREFERENCES por defecto
        properties.setCheckoutMode(MercadoPagoProperties.CheckoutIntegrationMode.PREFERENCES);
    }

    // =========================================================================
    // E2E-15: Intentos de registrar un providerOrderId duplicado
    // =========================================================================
    @Test
    @DisplayName("E2E-15: Restricción uq_payment_attempts_provider_order_id de PostgreSQL bloquea duplicados")
    void testE2E15_duplicateProviderOrderId_enforcedByPostgresUniqueConstraint() {
        BigDecimal amount = BigDecimal.valueOf(10.00);
        UUID res1 = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        UUID res2 = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));

        Payment p1 = createPendingPayment(res1, amount, buildValidSnapshot(amount));
        Payment p2 = createPendingPayment(res2, amount, buildValidSnapshot(amount));

        PaymentAttempt a1 = PaymentAttempt.create(p1.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-15-a", "ORDERS");
        a1.recordOrder("ORD-DUPLICATE-KEY-15", "https://mp.com/1");
        paymentAttemptRepository.save(a1);

        PaymentAttempt a2 = PaymentAttempt.create(p2.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-15-b", "ORDERS");
        a2.recordOrder("ORD-DUPLICATE-KEY-15", "https://mp.com/2");

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentAttemptRepository.save(a2);
        });
    }

}
